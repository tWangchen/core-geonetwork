package org.fao.geonet.doi.client;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.fao.geonet.domain.AbstractMetadata;
import org.fao.geonet.domain.ISODate;
import org.fao.geonet.kernel.datamanager.IMetadataManager;
import org.fao.geonet.kernel.datamanager.IMetadataSchemaUtils;
import org.fao.geonet.utils.Xml;
import org.jdom.Element;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import jeeves.server.context.ServiceContext;

/**
 * Class to register/unregister DOIs using the Datacite Metadata Store (Rest)
 * API.
 *
 * See https://support.datacite.org/docs/api
 *
 * @author Joseph, Geoscience Australia
 * 
 */
public class DoiRestManager extends DoiManager{

	public static final String DATACITE_XSL_CONVERSION_FILE = "formatter/datacite/to-json.xsl";
	public static final String DATACITE_JSON_SCHEMA_FILE = "formatter/datacite/jsonSchema.json";
    public static final String HTTPS_DOI_URL = "url";

    @Autowired
    IMetadataManager metadataManager;
    
    @Autowired
    IMetadataSchemaUtils schemaUtils;
    
    private DoiRestClient client;

	private JSONObject jsonSchema;
	
	@Override
	public void loadConfig() {
		super.loadConfig();
		if(initialised) {
			String serverUrl = sm.getValue(DoiSettings.SETTING_PUBLICATION_DOI_DOIURL);
            String username = sm.getValue(DoiSettings.SETTING_PUBLICATION_DOI_DOIUSERNAME);
            String password = sm.getValue(DoiSettings.SETTING_PUBLICATION_DOI_DOIPASSWORD);
            
			this.client = new DoiRestClient(serverUrl, username, password);
		}
	}

	@Override
	public Map<String, String> register(ServiceContext context, AbstractMetadata metadata, String eCatId)
			throws Exception {
		Map<String, String> doiInfo = getDoiInfo(eCatId);

		// The record in datacite format
		String dataciteJson = convertXmlToDataCiteFormat(metadata.getDataInfo().getSchemaId(),
				metadata.getXmlData(false), doiInfo);

		check(context, metadata, dataciteJson, eCatId, doiInfo);
		createDoi(context, metadata, doiInfo, dataciteJson, eCatId);
		return doiInfo;
	}

	public Map<String, String> update(ServiceContext context, AbstractMetadata metadata, String eCatId, String doi) throws Exception {
		Map<String, String> doiInfo = getDoiInfo(eCatId);

		// The record in datacite format
		String dataciteJson = convertXmlToDataCiteFormat(metadata.getDataInfo().getSchemaId(),
				metadata.getXmlData(false), doiInfo);
		
		validateAgainstJSONSchema(metadata, doi, dataciteJson);
		updateDoi(context, doiInfo, dataciteJson, eCatId);

		return doiInfo;
	}

	
	private void createDoi(ServiceContext context, AbstractMetadata metadata, Map<String, String> doiInfo,
			String dataciteJson, String eCatId) throws Exception {

		// Register the URL
		// 201 Created: operation successful;
		client.createDoi(doiInfo.get(DOI), dataciteJson, doiInfo.get(DOI_LANDING_URL));

		// Add the DOI in the record
		Element recordWithDoi = setDOIValue(doiInfo.get(DOI), metadata.getDataInfo().getSchemaId(),
				metadata.getXmlData(false));
		// Update the published copy
		// --- needed to detach md from the document
//        md.detach();

		metadataManager.updateMetadata(context, metadata.getId() + "", recordWithDoi, false, false, true, context.getLanguage(),
				new ISODate().toString(), true);

		doiInfo.put(DOI_ID, HTTPS_DOI_ORG + doiInfo.get(DOI));

	}
	
	private void updateDoi(ServiceContext context, Map<String, String> doiInfo,
			String dataciteJson, String eCatId) throws Exception {

		// Update
		String url = client.createUrl("dois/" + doiInfo.get(DOI));
		client.update(url, dataciteJson);
	}
	
	@Override
	public Map<String, Boolean> check(ServiceContext serviceContext, AbstractMetadata metadata, Object dataciteObj,
			String eCatId, Map<String, String> doiInfo) throws Exception {

		String dataciteJSON = null;
    	if(dataciteObj instanceof String)
    		dataciteJSON = (String) dataciteObj;
    	
    	if(doiInfo == null || doiInfo.isEmpty())
    		doiInfo = getDoiInfo(eCatId);
    	
		Map<String, Boolean> conditions = new HashMap<>();
		checkInitialised();
		conditions.put(DoiConditions.API_CONFIGURED, true);

		checkPreConditions(metadata, doiInfo.get(DOI));

		conditions.put(DoiConditions.RECORD_IS_PUBLIC, true);
		conditions.put(DoiConditions.STANDARD_SUPPORT, true);

		// ** Convert to DataCite format
		String dataciteFormatMetadata = dataciteJSON == null
				? convertXmlToDataCiteFormat(metadata.getDataInfo().getSchemaId(), metadata.getXmlData(false), doiInfo)
				: dataciteJSON;

		validateAgainstJSONSchema(metadata, doiInfo.get(DOI), dataciteFormatMetadata);
		conditions.put(DoiConditions.DATACITE_FORMAT_IS_VALID, true);
		return conditions;
	}

    
	/**
	 * Check conditions on DataCite side.
	 * 
	 * @param metadata
	 * @param doi
	 * @param dataciteMetadata
	 */
	protected void validateAgainstJSONSchema(AbstractMetadata metadata, String doi, String dataciteJSON)
			throws DoiClientException {
		// * DataCite API is up an running ?

		// JSON Schema validation
		
		try {
			if(jsonSchema == null) {
				Path jsonSchemaPath = schemaUtils.getSchemaDir(metadata.getDataInfo().getSchemaId())
						.resolve(DATACITE_JSON_SCHEMA_FILE);
				jsonSchema = new JSONObject(new JSONTokener(new FileReader(jsonSchemaPath.toFile())));
			}
		} catch (FileNotFoundException fne) {
			throw new DoiClientException("Unable to load the JSON schema file");
		} 
		
		try {
			JSONObject jsonSubject = new JSONObject(dataciteJSON);
			
			Schema schema = SchemaLoader.load(jsonSchema);
			schema.validate(jsonSubject);
		} catch (ValidationException e) {

			//String violation = e.getCausingExceptions().stream().map(ValidationException::getViolatedSchema).map(Schema::getTitle).collect(Collectors.joining(", "));
			throw new DoiClientException(String.format("Record '%s' converted to DataCite format is invalid. "
					+ "Attributes with minimum fields required (identifier, creators, titles, publisher, publicationYear, resourceType).", metadata.getUuid()));
		}
		
		// * MDS / DOI does not exist already
        // curl -i --user username:password https://api.test.datacite.org/doi/10.5072/GN
        // Return 404
        final String doiResponse = client.retrieveDoi(doi);
        if (doiResponse != null) {
            throw new DoiClientException(String.format(
                "Record '%s' looks to be already published on DataCite on DOI '%s'." +
                    "If the DOI is not correct, remove it from the record and ask for a new one.",
                metadata.getUuid(), doi));
        }
        
		
	}

	private String convertXmlToDataCiteFormat(String schema, Element md, Map<String, String> doiInfo) throws Exception {
		final Path styleSheet = schemaUtils.getSchemaDir(schema).resolve(DATACITE_XSL_CONVERSION_FILE);
		final boolean exists = Files.exists(styleSheet);
		if (!exists) {
			throw new DoiClientException(String.format(
					"To create a DOI, the record needs to be converted to the DataCite format (https://schema.datacite.org/). You need to create a formatter for this in schema_plugins/%s/%s. If the standard is a profile of ISO19139, you can simply point to the ISO19139 formatter.",
					schema, DATACITE_XSL_CONVERSION_FILE));
		}
		;

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		Map<String, Object> params = new HashMap<String, Object>();

		params.put(DOI_PREFIX_PARAMETER, this.doiPrefix);
		params.put(HTTPS_DOI_URL, doiInfo.get(DOI_LANDING_URL));

		Xml.transformXml(md, styleSheet, stream, params);
		String jsonContent = new String(stream.toByteArray());
		
		JsonParser parser = new JsonParser();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
		try {
			JsonElement el = parser.parse(jsonContent.replace("\\", "\\\\"));
			jsonContent = gson.toJson(el);
		}catch(Exception e) {
			throw new DoiClientException("Contains special character in abstract or title that cannot be processed at this stage. Remove the special characters or mint DOI manually from Datacite");
		}
		return jsonContent;
	}
	
}
