package org.fao.geonet;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
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
import org.fao.geonet.kernel.SchemaManager;
import org.fao.geonet.kernel.UpdateDatestamp;
import org.fao.geonet.kernel.datamanager.IMetadataIndexer;
import org.fao.geonet.kernel.datamanager.IMetadataManager;
import org.fao.geonet.kernel.setting.SettingManager;
import org.fao.geonet.kernel.setting.Settings;
import org.fao.geonet.repository.MetadataRepository;
import org.fao.geonet.utils.Log;
import org.jdom.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.base.Joiner;

import jeeves.server.context.ServiceContext;
import jeeves.server.dispatchers.ServiceManager;

public class ScheduledTasks {

	private final static String INTERNAL = "Internal";
	private final static String INTERNAL_CONTACT = "Internal Contact";
	
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

		Log.info(Geonet.GA, "Executing Internal Contacts at " + Instant.now().toString());
		InputStream objectData = null;
		try {
			ApplicationContextHolder.set(applicationContext);
			context = serviceManager.createServiceContext("manageUserList", applicationContext);
		}catch(Exception e) {
			Log.error(Geonet.DATA_MANAGER, "No service context");
		}
		
		try {
			AmazonS3URI s3uri = new AmazonS3URI(settingManager.getValue(Settings.SYSTEM_INTERNAL_CONTACT_AWS_URL));
			AmazonS3 s3client = AmazonS3ClientBuilder.standard().withRegion(s3uri.getRegion()).build();
			
			try {
				S3Object object = s3client.getObject(new GetObjectRequest(s3uri.getBucket(), s3uri.getKey()));
				objectData = object.getObjectContent();
			}catch(AmazonS3Exception e) {
				Log.error(Geonet.DATA_MANAGER, "Could find the contact list url: "  + e.getErrorMessage());
			}
			
			if(objectData != null) {
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
				
				ci_ind.addContent(name);				
				ci_ind.addContent(contactInfo);				
				ci_ind.addContent(position);
				
				party.addContent(ci_ind);
				ci_resp.addContent(role);
				ci_resp.addContent(party);
				
				
				try {
					for (CSVRecord record : parser) {
						String dn =  record.get("sn") + ", " + record.get("givenName").charAt(0) + ".";
						//String title = record.get("Title");
						String company = record.get("company");
						
						String uid = record.get("samAccountName");
						userIds.add(uid);
						

						boolean ufo = false, indexImmediate = true;
						try {
							
							Metadata md = metadataRepository.findOneByUuid(uid);
							//String extra = Joiner.on("|").join(INTERNAL, title, company);
							String extra = Joiner.on("|").join(INTERNAL, company);
							
							if(md == null || (md != null && !md.getDataInfo().getExtra().equals(extra))) {
								Element resp = ci_resp.getChild("party", Namespaces.CIT).getChild("CI_Individual", Namespaces.CIT);
								resp.getChild("name", Namespaces.CIT).getChild("CharacterString", Namespaces.GCO_3).setText(dn);
								resp.getChild("contactInfo", Namespaces.CIT).getChild("CI_Contact", Namespaces.CIT).getChild("contactInstructions", Namespaces.CIT)
									.getChild("CharacterString", Namespaces.GCO_3).setText(company);
								//resp.getChild("positionName", Namespaces.CIT).getChild("CharacterString", Namespaces.GCO_3).setText(title);
							}
							
							if (md == null) {
								
								AbstractMetadata newMetadata = newMetadata(uid, extra, ci_resp.getQualifiedName());
								
								Log.info(Geonet.GA, String.format("Adding user %s, %s", dn, company));
								metadataManager.insertMetadata(context, newMetadata, ci_resp, false, indexImmediate, ufo, UpdateDatestamp.NO, false, true);
								
							} else {
								
								if(!md.getDataInfo().getExtra().equals(extra)) {
								
									AbstractMetadata newMetadata = newMetadata(uid, extra, ci_resp.getQualifiedName());
									Log.info(Geonet.GA, String.format("Updating user %s, %s. Changing role from %s to %s.", dn, company, md.getDataInfo().getExtra(), extra));
									
									metadataManager.deleteMetadata(context, String.valueOf(md.getId()));
									metadataManager.insertMetadata(context, newMetadata, ci_resp, false, indexImmediate, ufo, UpdateDatestamp.NO, false, true);
								}
								
							}

						} catch (Exception e) {
							Log.error(Geonet.GA, "Error while add the user list " + e);
						}
						
					}
				} finally {
					parser.close();
					reader.close();
				}

				ci_resp.detach();
				
				List<String> existingUserIds = metadataRepository.findAllByDataInfo_ExtraStartsWith(INTERNAL).stream().map(m -> m.getUuid()).collect(Collectors.toList()); 
				existingUserIds.removeAll(userIds);
				
				try {
					if(existingUserIds.size() > 0) {
						Log.info(Geonet.GA, "Removing Users: " + Arrays.toString(existingUserIds.toArray()));
						
						
						String date = new ISODate().toString();
						
						for (String uuid : existingUserIds) {
							
							Metadata removeMd = metadataRepository.findOneByUuid(uuid);
							
							//After deleting Internal Contacts, update those contacts as External
							Element xmlElement = schemaManager.transformInternalSubtemplate(removeMd.getXmlData(false));
							metadataManager.deleteMetadata(context, String.valueOf(removeMd.getId()));
							
							try {
					            String metadataId = metadataManager.insertMetadata(context, "iso19115-3", xmlElement, UUID.randomUUID().toString(),
					                    1, String.valueOf(ReservedGroup.all.getId()), settingManager.getSiteId(), MetadataType.SUB_TEMPLATE.codeString, null, null, date, date, false, true);
					            
					            Log.info(Geonet.GA, String.format("Changed user % from internal to external contact. metadataId %s ", uuid, metadataId));
					            
					            
					        } catch (DataIntegrityViolationException ex) {
					        	Log.error(Geonet.GA, "Error while updating Internal contact to External, " + ex.getMessage());
					        } catch (Exception ex) {
					        	Log.error(Geonet.GA, "Error while updating Internal contact to External, " + ex.getMessage());
					        }
							
							
						}
					}
					
				}catch(Exception e) {
					Log.error(Geonet.GA, "Unable to delete terminated userIds, " + e.getMessage());
				}
				
			}
			
		} catch (IOException e) {
			Log.error(Geonet.GA, "Unable access user list, " + e.getMessage());
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
		contactTypeStr.addContent(INTERNAL_CONTACT);
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
