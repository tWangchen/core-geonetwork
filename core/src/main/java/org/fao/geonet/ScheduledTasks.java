package org.fao.geonet;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.constants.Geonet.Namespaces;
import org.fao.geonet.domain.AbstractMetadata;
import org.fao.geonet.domain.ISODate;
import org.fao.geonet.domain.Metadata;
import org.fao.geonet.domain.MetadataType;
import org.fao.geonet.domain.ReservedGroup;
import org.fao.geonet.domain.Setting;
import org.fao.geonet.kernel.SchemaManager;
import org.fao.geonet.kernel.UpdateDatestamp;
import org.fao.geonet.kernel.datamanager.IMetadataManager;
import org.fao.geonet.kernel.setting.SettingManager;
import org.fao.geonet.kernel.setting.Settings;
import org.fao.geonet.repository.MetadataRepository;
import org.fao.geonet.utils.Log;
import org.jdom.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Lazy;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.base.Joiner;

import jeeves.server.context.ServiceContext;
import jeeves.server.dispatchers.ServiceManager;

public class ScheduledTasks {

	private final static String INTERNAL = "Internal";
	@Autowired
	SchemaManager schemaManager;

	@Autowired
	IMetadataManager metadataManager;

	@Autowired
	MetadataRepository metadataRepository;

	@Autowired
	protected ConfigurableApplicationContext applicationContext;

	@Autowired
	protected ServiceManager serviceManager;
	
	private ServiceContext context;
	
	@Autowired
    @Lazy
    private SettingManager settingManager;

	
	public void manageUserList() {

		try {
			ApplicationContextHolder.set(applicationContext);
			context = serviceManager.createServiceContext("manageUserList", applicationContext);
		}catch(Exception e) {
			Log.error(Geonet.DATA_MANAGER, "No service context");
		}
		
		try {
			AmazonS3URI s3uri = new AmazonS3URI(settingManager.getValue(Settings.SYSTEM_INTERNAL_CONTACT_AWS_URL));
			AmazonS3 s3client = AmazonS3ClientBuilder.standard().withRegion(s3uri.getRegion()).build();
			
			S3Object object = s3client.getObject(new GetObjectRequest(s3uri.getBucket(), s3uri.getKey()));
			InputStream objectData = object.getObjectContent();
			
			final Reader reader = new InputStreamReader(objectData);

			CSVParser parser = new CSVParser(reader, CSVFormat.EXCEL.withHeader().withIgnoreEmptyLines());
			Element role = getRoleElement();
			Element contactInfo = getContactInfoElement();
			Element name = getNameElement();
			Element position = getPositionNameElement();

			Element ci_resp = new Element("CI_Responsibility", Namespaces.CIT);
			Element party = new Element("party", Namespaces.CIT);
			Element ci_ind = new Element("CI_Individual", Namespaces.CIT);

			//List<String> userIds = parser.getRecords().stream().map(record -> record.get("samAccountName")).collect(Collectors.toList());
			List<String> userIds = new ArrayList<>();
			
			
			try {
				for (CSVRecord record : parser) {
					String dn =  record.get("sn") + ", " + record.get("givenName").charAt(0) + ".";
					String title = record.get("Title");
					String company = record.get("company");
					
					String uid = record.get("samAccountName");
					userIds.add(uid);
					
					name.getChild("CharacterString", Namespaces.GCO_3).setText(dn);
					ci_ind.addContent(name);

					contactInfo.getChild("CI_Contact", Namespaces.CIT).getChild("contactInstructions", Namespaces.CIT)
							.getChild("CharacterString", Namespaces.GCO_3).setText(company);
					ci_ind.addContent(contactInfo);

					position.getChild("CharacterString", Namespaces.GCO_3).setText(title);
					ci_ind.addContent(position);
					
					party.addContent(ci_ind);
					ci_resp.addContent(role);
					ci_resp.addContent(party);

					boolean ufo = false, indexImmediate = true;
					try {
						
						Metadata md = metadataRepository.findOneByUuid(uid);
						String extra = Joiner.on("|").join(INTERNAL, title, company);
						
						if (md == null) {
							
							AbstractMetadata newMetadata = newMetadata(uid, extra, ci_resp.getQualifiedName());

							Log.info(Geonet.DATA_MANAGER, String.format("Adding user %s, %s, %s", dn, title, company));
							metadataManager.insertMetadata(context, newMetadata, ci_resp, false, indexImmediate, ufo, UpdateDatestamp.NO, false, true);
							
							
						} else {
							
							if(!md.getDataInfo().getExtra().equals(extra)) {
								
								AbstractMetadata newMetadata = newMetadata(uid, extra, ci_resp.getQualifiedName());
								Log.info(Geonet.DATA_MANAGER, String.format("Updating user %s, %s, %s. Changing role from %s to %s.", dn, title, company, md.getDataInfo().getExtra(), extra));
								
								metadataManager.deleteMetadata(context, String.valueOf(md.getId()));
								metadataManager.insertMetadata(context, newMetadata, ci_resp, false, indexImmediate, ufo, UpdateDatestamp.NO, false, true);
								
							}
							
						}

					} catch (Exception e) {
						Log.error(Geonet.DATA_MANAGER, "Error while add the user list " + e);
					}

					contactInfo.detach();
					role.detach();
					name.detach();

					position.detach();
					ci_ind.detach();
					party.detach();
					// ci_resp.detach();
					
				}
			} finally {
				parser.close();
				reader.close();
			}
			
			List<String> existingUserIds = metadataRepository.findAllByDataInfo_ExtraStartsWith(INTERNAL).stream().map(m -> m.getUuid()).collect(Collectors.toList()); 
			existingUserIds.removeAll(userIds);
			
			try {
				if(existingUserIds.size() > 0) {
					Log.info(Geonet.DATA_MANAGER, "Removing Users: " + Arrays.toString(existingUserIds.toArray()));
					
					for (String uuid : existingUserIds) {
						metadataManager.deleteMetadata(context, String.valueOf(metadataRepository.findOneByUuid(uuid).getId()));
					}
				}
				
			}catch(Exception e) {
				Log.error(Geonet.DATA_MANAGER, "Unable to delete terminated userIds, " + e);
			}
			
		} catch (IOException e) {
			Log.error(Geonet.DATA_MANAGER, "Unable access user list, " + e);
		}

	}

	private AbstractMetadata newMetadata(String uid, String extra, String qn) {
		final AbstractMetadata newMetadata = new Metadata();
        newMetadata.setUuid(uid);
        newMetadata.getDataInfo()
        	.setChangeDate(new ISODate())
        	.setCreateDate(new ISODate())
        	.setExtra(extra)
        	.setRoot(qn)
        	.setSchemaId(Geonet.SCHEMA_ISO_19115_3)
            .setType(MetadataType.SUB_TEMPLATE);
        newMetadata.getSourceInfo()
        	.setGroupOwner(ReservedGroup.all.getId())
        	.setOwner(1).setSourceId(INTERNAL);
        
        
        return newMetadata;
	}
	private static Element getRoleElement() {

		Element role = new Element("role", Namespaces.CIT);
		Element roleCode = new Element("CI_RoleCode", Namespaces.CIT);
		roleCode.setAttribute("codeList", "codeListLocation#CI_RoleCode");
		roleCode.setAttribute("codeListValue", "");

		role.addContent(roleCode);

		return role;

	}

	private static Element getContactInfoElement() {

		Element contactInfo = new Element("contactInfo", Namespaces.CIT);
		Element ci_contact = new Element("CI_Contact", Namespaces.CIT);

		Element contactInstructions = new Element("contactInstructions", Namespaces.CIT);
		Element contactInstructionsStr = new Element("CharacterString", Namespaces.GCO_3);
		contactInstructionsStr.addContent("");
		contactInstructions.addContent(contactInstructionsStr);

		Element contactType = new Element("contactType", Namespaces.CIT);
		Element contactTypeStr = new Element("CharacterString", Namespaces.GCO_3);
		contactTypeStr.addContent("Internal Contact");
		contactType.addContent(contactTypeStr);

		ci_contact.addContent(contactInstructions);
		ci_contact.addContent(contactType);
		contactInfo.addContent(ci_contact);

		return contactInfo;

	}

	private static Element getNameElement() {

		Element name = new Element("name", Namespaces.CIT);
		Element characterString = new Element("CharacterString", Namespaces.GCO_3);
		characterString.addContent("");
		name.addContent(characterString);

		return name;

	}

	private static Element getPositionNameElement() {

		Element positionName = new Element("positionName", Namespaces.CIT);
		Element characterString = new Element("CharacterString", Namespaces.GCO_3);
		characterString.addContent("");
		positionName.addContent(characterString);

		return positionName;

	}

}
