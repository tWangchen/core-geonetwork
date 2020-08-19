package org.fao.geonet.doi.client;

import static org.fao.geonet.doi.client.DoiSettings.LOGGER_NAME;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.fao.geonet.ApplicationContextHolder;
import org.fao.geonet.api.exception.ResourceNotFoundException;
import org.fao.geonet.domain.AbstractMetadata;
import org.fao.geonet.domain.ISODate;
import org.fao.geonet.kernel.DataManager;
import org.fao.geonet.kernel.datamanager.IMetadataManager;
import org.fao.geonet.kernel.datamanager.IMetadataSchemaUtils;
import org.fao.geonet.kernel.datamanager.base.BaseMetadataSchemaUtils;
import org.fao.geonet.kernel.schema.MetadataSchema;
import org.fao.geonet.kernel.setting.SettingManager;
import org.fao.geonet.utils.Log;
import org.fao.geonet.utils.Xml;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;

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
public class DoiRestManager {

	private static final String DOI_ADD_XSL_PROCESS = "process/doi-add.xsl";
    private static final String DOI_REMOVE_XSL_PROCESS = "process/doi-remove.xsl";
    public static final String DATACITE_XSL_CONVERSION_FILE = "formatter/datacite/to-json.xsl";
	public static final String DATACITE_JSON_SCHEMA_FILE = "formatter/datacite/jsonSchema.json";
    public static final String DOI_PREFIX_PARAMETER = "doiPrefix";
    public static final String HTTPS_DOI_ORG = "https://dx.doi.org/";
    public static final String HTTPS_DOI_URL = "url";

    @Autowired
    IMetadataManager metadataManager;
    
    @Autowired
    IMetadataSchemaUtils schemaUtils;
    
    private DoiRestClient client;
    protected String doiPrefix;
    protected String landingPageTemplate;
    protected boolean initialised = false;

    //DataManager dm;
    SettingManager sm;

    public static final String DOI_GET_SAVED_QUERY = "doi-get";
	
	private JSONObject jsonSchema;
	
	public DoiRestManager() {
        sm = ApplicationContextHolder.get().getBean(SettingManager.class);

        loadConfig();
    }

	public void loadConfig() {
		initialised = false;
		if (sm != null) {

			String serverUrl = sm.getValue(DoiSettings.SETTING_PUBLICATION_DOI_DOIURL);
			String username = sm.getValue(DoiSettings.SETTING_PUBLICATION_DOI_DOIUSERNAME);
			String password = sm.getValue(DoiSettings.SETTING_PUBLICATION_DOI_DOIPASSWORD);

			doiPrefix = sm.getValue(DoiSettings.SETTING_PUBLICATION_DOI_DOIKEY);
			landingPageTemplate = sm.getValue(DoiSettings.SETTING_PUBLICATION_DOI_LANDING_PAGE_TEMPLATE);

			final boolean emptyUrl = StringUtils.isEmpty(serverUrl);
			final boolean emptyUsername = StringUtils.isEmpty(username);
			final boolean emptyPassword = StringUtils.isEmpty(password);
			final boolean emptyPrefix = StringUtils.isEmpty(doiPrefix);
			if (emptyUrl || emptyUsername || emptyPassword || emptyPrefix) {
				StringBuilder report = new StringBuilder(
						"DOI configuration is not complete. Check in System Configuration to fill the DOI configuration.");
				if (emptyUrl) {
					report.append("\n* URL MUST be set");
				}
				if (emptyUsername) {
					report.append("\n* Username MUST be set");
				}
				if (emptyPassword) {
					report.append("\n* Password MUST be set");
				}
				if (emptyPrefix) {
					report.append("\n* Prefix MUST be set");
				}
				Log.warning(DoiSettings.LOGGER_NAME, report.toString());
				this.initialised = false;
			} else {
				Log.debug(DoiSettings.LOGGER_NAME, "DOI configuration looks perfect.");
				// TODO: Check connection ?
				this.client = new DoiRestClient(serverUrl, username, password);
				initialised = true;
			}
		}
	}

	public Map<String, String> register(ServiceContext context, AbstractMetadata metadata, String eCatId)
			throws Exception {
		Log.debug(LOGGER_NAME, ">>>>>> DOIRestManager --> register <-- " );
		Map<String, String> doiInfo = new HashMap<>(3);
		// The new DOI for this record
		String doi = DoiBuilder.create(this.doiPrefix, eCatId);
		doiInfo.put("doi", doi);

		// The record in datacite format
		String dataciteJson = convertXmlToDataCiteFormat(metadata.getDataInfo().getSchemaId(),
				metadata.getXmlData(false));

		check(context, metadata, dataciteJson, eCatId);
		createDoi(context, metadata, doiInfo, dataciteJson, eCatId);
		Log.debug(LOGGER_NAME, ">>>>>> DOIRestManager --> after register <-- " );
		return doiInfo;
	}

	public Map<String, String> update(ServiceContext context, AbstractMetadata metadata, String eCatId, String doi) throws Exception {
		Log.debug(LOGGER_NAME, ">>>>>> DOIRestManager --> update <-- " );
		Map<String, String> doiInfo = new HashMap<>(3);
		// The new DOI for this record
		String doiId = doi.substring(doi.indexOf(this.doiPrefix));
		doiInfo.put("doi", doiId);

		// The record in datacite format
		String dataciteJson = convertXmlToDataCiteFormat(metadata.getDataInfo().getSchemaId(),
				metadata.getXmlData(false));

		
		validateAgainstJSONSchema(metadata, doi, dataciteJson);
		updateDoi(context, doiInfo, dataciteJson, eCatId);

		return doiInfo;
	}

	
	private void createDoi(ServiceContext context, AbstractMetadata metadata, Map<String, String> doiInfo,
			String dataciteJson, String eCatId) throws Exception {
		Log.debug(LOGGER_NAME, ">>>>>> DOIRestManager --> createDoi <-- " );
		// Register the URL
		// 201 Created: operation successful;
		String landingPage = landingPageTemplate.replace("{{eCatId}}", eCatId);
		doiInfo.put("doiLandingPage", landingPage);
		client.createDoi(doiInfo.get("doi"), dataciteJson, landingPage);

		// Add the DOI in the record
		Element recordWithDoi = setDOIValue(doiInfo.get("doi"), metadata.getDataInfo().getSchemaId(),
				metadata.getXmlData(false));
		// Update the published copy
		// --- needed to detach md from the document
//        md.detach();

		metadataManager.updateMetadata(context, metadata.getId() + "", recordWithDoi, false, false, true, context.getLanguage(),
				new ISODate().toString(), true);

		doiInfo.put("doiUrl", HTTPS_DOI_ORG + doiInfo.get("doi"));

	}
	
	private void updateDoi(ServiceContext context, Map<String, String> doiInfo,
			String dataciteJson, String eCatId) throws Exception {
		Log.debug(LOGGER_NAME, ">>>>>> DOIRestManager --> updateDoi <-- " );
		// Update
		String url = client.createUrl("dois/" + doiInfo.get("doi"));
		Log.debug(LOGGER_NAME, ">>>>>> DOIRestManager --> updateDoi <-- url: " + url);
		client.update(url, dataciteJson);

		

	}
	
	/**
     * Sets the DOI URL value in the metadata record using the process DOI_ADD_XSL_PROCESS.
     *
     */
    public Element setDOIValue(String doi, String schema, Element md) throws Exception {
        Path styleSheet = schemaUtils.getSchemaDir(schema).resolve(DOI_ADD_XSL_PROCESS);
        boolean exists = Files.exists(styleSheet);
        if (!exists) {
            throw new DoiClientException(String.format("To create a DOI, the schema has to defined how to insert a DOI in the record. The schema_plugins/%s/process/%s was not found. Create the XSL transformation.",
                schema, DOI_ADD_XSL_PROCESS));
        }

        Map<String, Object> params = new HashMap<>(1);
        params.put("doi", HTTPS_DOI_ORG + doi);
        return Xml.transform(md, styleSheet, params);
    }
    
	
	public Map<String, Boolean> check(ServiceContext serviceContext, AbstractMetadata metadata, String dataciteJSON,
			String eCatId) throws Exception {

		Log.debug(LOGGER_NAME, "   -- DOIRestManager >> check method");
		Map<String, Boolean> conditions = new HashMap<>();
		checkInitialised();
		conditions.put(DoiConditions.API_CONFIGURED, true);

		Log.debug(LOGGER_NAME, "   -- DOIRestManager >> doiPrefix: " + this.doiPrefix);

		String doi = DoiBuilder.create(this.doiPrefix, eCatId);
		checkPreConditions(metadata, doi);
		Log.debug(LOGGER_NAME, "   -- DOIRestManager >> checking pre conditions...");
		conditions.put(DoiConditions.RECORD_IS_PUBLIC, true);
		conditions.put(DoiConditions.STANDARD_SUPPORT, true);

		// ** Convert to DataCite format
		String dataciteFormatMetadata = dataciteJSON == null
				? convertXmlToDataCiteFormat(metadata.getDataInfo().getSchemaId(), metadata.getXmlData(false))
				: dataciteJSON;

		validateAgainstJSONSchema(metadata, doi, dataciteFormatMetadata);
		conditions.put(DoiConditions.DATACITE_FORMAT_IS_VALID, true);
		return conditions;
	}

	/**
     * Check DOI conditions on current records.
     *
     * @param metadata
     * @param doi
     * @throws DoiClientException
     * @throws IOException
     * @throws JDOMException
     */
    protected void checkPreConditions(AbstractMetadata metadata, String doi) throws DoiClientException, IOException, JDOMException {

    	// Record MUST not contains a DOI
        final MetadataSchema schema = schemaUtils.getSchema(metadata.getDataInfo().getSchemaId());
        Element xml = metadata.getXmlData(false);
        try {
            String currentDoi = schema.queryString(DOI_GET_SAVED_QUERY, xml);
            if (StringUtils.isNotEmpty(currentDoi)) {
                // Current doi does not match the one going to be inserted. This is odd
                if (!currentDoi.equals(HTTPS_DOI_ORG + doi)) {
                    throw new DoiClientException(String.format(
                        "Record '%s' already contains a DOI '%s' which is not equal " +
                            "to the DOI about to be created (ie. '%s'). " +
                            "Maybe current DOI does not correspond to that record? " +
                            "This may happen when creating a copy of a record having " +
                            "an existing DOI.",
                        metadata.getUuid(), currentDoi, HTTPS_DOI_ORG + doi));
                }

                throw new DoiClientException(String.format(
                    "Record '%s' already contains a DOI. The DOI is '%s'. " +
                        "We cannot register it again. " +
                        "Remove the DOI reference if it does not apply to that record.",
                    metadata.getUuid(), currentDoi));
            }
        } catch (ResourceNotFoundException e) {
            // Schema not supporting DOI extraction and needs to be configured
            // Check bean configuration which should contains something like
            //            <bean class="org.fao.geonet.kernel.schema.SavedQuery">
            //              <property name="id" value="doi-get"/>
            //              <property name="xpath"
            //                value="*//gmd:CI_OnlineResource[gmd:protocol/gco:CharacterString = 'WWW:DOI']/gmd:URL/text()"/>
            //            </bean>
            throw new DoiClientException(String.format(
                "Record '%s' is in schema '%s' and we cannot find a saved query " +
                    "with id '%s' to retrieve the DOI. Error is %s. " +
                    "Check the schema %sSchemaPlugin and add the DOI get query.",
                metadata.getUuid(), schema.getName(),
                DOI_GET_SAVED_QUERY, e.getMessage(),
                schema.getName()));
        }
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
				Log.debug(LOGGER_NAME, "   -- DOIRestManager >> validateAgainstJSONSchema, jsonSchemaPath: "+jsonSchemaPath.toString());
				jsonSchema = new JSONObject(new JSONTokener(new FileReader(jsonSchemaPath.toFile())));
			}
		} catch (FileNotFoundException fne) {
			throw new DoiClientException("Unable to load the JSON schema file");
		} 
		
		try {
			JSONObject jsonSubject = new JSONObject(dataciteJSON);
			
			Log.debug(LOGGER_NAME, "   -- DOIRestManager >> validateAgainstJSONSchema, jsonSubject: " + jsonSubject.toString());
			
			Schema schema = SchemaLoader.load(jsonSchema);
			schema.validate(jsonSubject);
			Log.debug(LOGGER_NAME, "   -- DOIRestManager >> validateAgainstJSONSchema, schema outcome " + schema.getDescription());
		} catch (ValidationException e) {

			String violation = e.getCausingExceptions().stream().map(ValidationException::getViolatedSchema).map(Schema::getTitle).collect(Collectors.joining(","));
			throw new DoiClientException(String.format("Record '%s' converted to DataCite format is invalid."
					+ "Missing fields in DataCite are: %s, exception is %s", metadata.getUuid(), violation, e.getMessage()));
		}
		
	}

	private String convertXmlToDataCiteFormat(String schema, Element md) throws Exception {
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
		Log.debug(LOGGER_NAME, "   -- DOIRestManager >> convertXmlToDataCiteFormat, doiPrefix: "+doiPrefix);
		params.put(DOI_PREFIX_PARAMETER, this.doiPrefix);
		params.put(HTTPS_DOI_URL, this.client.serverUrl);

		Xml.transformXml(md, styleSheet, stream, params);
		String jsonContent = new String(stream.toByteArray());
		
		//Log.debug(LOGGER_NAME, "   -- DOIRestManager >> convertXmlToDataCiteFormat, jsonContent: "+jsonContent);
		
		return jsonContent;
	}
	
	protected void checkInitialised() throws DoiClientException {
        if (!initialised) {
            throw new DoiClientException("DOI configuration is not complete. Check System Configuration and set the DOI configuration.");
        }
    }
}
