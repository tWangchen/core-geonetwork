package org.fao.geonet.api.records;

import static org.fao.geonet.api.ApiParams.API_CLASS_RECORD_OPS;
import static org.fao.geonet.api.ApiParams.API_CLASS_RECORD_TAG;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.fao.geonet.api.API;
import org.fao.geonet.api.ApiParams;
import org.fao.geonet.api.ApiUtils;
import org.fao.geonet.api.exception.FeatureNotEnabledException;
import org.fao.geonet.api.records.model.MetadataStatusParameter;
import org.fao.geonet.api.tools.i18n.LanguageUtils;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.doi.client.DoiManager;
import org.fao.geonet.domain.AbstractMetadata;
import org.fao.geonet.domain.Constants;
import org.fao.geonet.domain.ISODate;
import org.fao.geonet.domain.MetadataStatus;
import org.fao.geonet.domain.MetadataStatusId;
import org.fao.geonet.domain.Profile;
import org.fao.geonet.domain.StatusValue;
import org.fao.geonet.domain.StatusValueType;
import org.fao.geonet.kernel.AccessManager;
import org.fao.geonet.kernel.ECatOperationManager;
import org.fao.geonet.kernel.SchemaManager;
import org.fao.geonet.kernel.SelectionManager;
import org.fao.geonet.kernel.TransformManager;
import org.fao.geonet.kernel.datamanager.IMetadataIndexer;
import org.fao.geonet.kernel.datamanager.IMetadataManager;
import org.fao.geonet.kernel.datamanager.IMetadataOperations;
import org.fao.geonet.kernel.datamanager.IMetadataSchemaUtils;
import org.fao.geonet.kernel.metadata.StatusActions;
import org.fao.geonet.kernel.metadata.StatusActionsFactory;
import org.fao.geonet.kernel.schema.AssociatedResource;
import org.fao.geonet.kernel.schema.AssociatedResourcesSchemaPlugin;
import org.fao.geonet.kernel.schema.SchemaPlugin;
import org.fao.geonet.kernel.setting.SettingManager;
import org.fao.geonet.kernel.setting.Settings;
import org.fao.geonet.repository.StatusValueRepository;
import org.fao.geonet.utils.Log;
import org.fao.geonet.utils.Xml;
import org.jdom.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import jeeves.server.UserSession;
import jeeves.server.context.ServiceContext;
import jeeves.services.ReadWriteController;
import springfox.documentation.annotations.ApiIgnore;
import thredds.inventory.bdb.MetadataManager;

@RequestMapping(value = {
	    "/{portal}/api/records",
	    "/{portal}/api/" + API.VERSION_0_1 +
	        "/records"
})
@Api(value = API_CLASS_RECORD_TAG,
    tags = API_CLASS_RECORD_TAG,
    description = API_CLASS_RECORD_OPS)
@Controller("recordRetire")
@ReadWriteController
public class MetadataRetireApi {
	
	private Map<String, String> report = new HashMap<>();

	static final String RETIRE_REPORT = "retire_report";
	static final String RETIRE_STATUS = "retire_status";

	@Autowired
    SettingManager settingManager;
	
	@Autowired
    AccessManager accessManager;
	
	@Autowired
    LanguageUtils languageUtils;

	@Autowired
    StatusActionsFactory statusActionFactory;
	
	@Autowired
    IMetadataIndexer metadataIndexer;
	
	@Autowired
	StatusValueRepository statusValueRepository;
	
	@Autowired
	ECatOperationManager opManager;
	
	@Autowired
	ApplicationContext context;
	
	@Autowired
    TransformManager transManager;
	
	@Autowired
    SchemaManager schemaManager;
	
	@Autowired
    private IMetadataManager metadataManager;
	
	@Autowired
	IMetadataSchemaUtils metadataSchemaUtils;
	
	@Autowired
    private IMetadataOperations metadataOperations;
	
	@ApiOperation(value = "Bulk retire records.", nickname = "retireRecords")
	@RequestMapping(value = "/retire", method = RequestMethod.PUT)
	@PreAuthorize("hasRole('Administrator')")
	@ApiResponses(value = { @ApiResponse(code = 204, message = "Retire Records."),
			@ApiResponse(code = 404, message = "Metadata not found."),
			@ApiResponse(code = 500, message = "Service unavailable."),
			@ApiResponse(code = 403, message = ApiParams.API_RESPONSE_NOT_ALLOWED_ONLY_ADMIN) })
	public ResponseEntity retireRecords(@RequestParam(required = false) String bucket, HttpServletRequest request,
			@ApiParam(hidden = true) @ApiIgnore HttpSession session) {

		if (report != null && !report.isEmpty())
			report.clear();
        
		request.getSession().setAttribute(RETIRE_STATUS, false);
		ServiceContext context = ApiUtils.createServiceContext(request);
		Runnable task = () -> {
			retireAllRecords(context, bucket, session);
		};

		// start the thread
		new Thread(task).start();

		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	private void retireAllRecords(ServiceContext serviceContext, String bucket, HttpSession session) {
	
		try {

			UserSession userSession = serviceContext.getUserSession();
			SelectionManager sm = SelectionManager.getManager(userSession);

			Set<String> uuids = new HashSet<>();
			if (sm != null) {

				synchronized (sm.getSelection(bucket)) {
					for (Iterator<String> iter = sm.getSelection(bucket).iterator(); iter.hasNext();) {
						String uuid = (String) iter.next();
						uuids.add(uuid);
					}
				}
			}

			uuids.stream().forEach(uuid -> {
				String eCatId = "";
				try {

					AbstractMetadata metadata = ApiUtils.getRecord(uuid);
					if (metadata.getDataInfo().getType().codeString.equals(String.valueOf(Constants.YN_FALSE))) {
						eCatId = opManager.getECatIdFromUUID(context, uuid);
						retire(uuid, serviceContext);
						report.put(eCatId, "Successfully retired record");

					} else {
						report.put(eCatId, "Not a metadata record. Selected record ( " + uuid
								+ " ) seems to be Template/Sub directory.");
					}
				} catch (Exception rnfe) {
					report.put(eCatId, rnfe.getMessage());
				}
			});

		} catch (Exception e) {
			Log.error(Geonet.SCHEMA_MANAGER, " Bulk Retiring of records failed, Error is " + e.getMessage());
		} finally {
			session.setAttribute(RETIRE_REPORT, report);
			session.setAttribute(RETIRE_STATUS, true);
		}

	}

	private boolean retire(String metadataUuid, ServiceContext srvContext) throws Exception {
		
		AbstractMetadata metadata = ApiUtils.getRecord(metadataUuid);
		
        UserSession us = srvContext.getUserSession();
        String schema = metadataSchemaUtils.getMetadataSchema(String.valueOf(metadata.getId()));
        
		boolean isMdWorkflowEnable = settingManager.getValueAsBool(Settings.METADATA_WORKFLOW_ENABLE);

        if (!isMdWorkflowEnable) {
            throw new FeatureNotEnabledException(
                    "Metadata workflow is disabled, can not be set the status of metadata");
        }
        
        removeAssociations(metadata, schema, srvContext);
        
        MetadataStatusParameter status = new MetadataStatusParameter();
        status.setStatus(Integer.parseInt(StatusValue.Status.RETIRED));
        status.setCloseDate(new ISODate().toString());
        status.setOwner(us.getUserIdAsInt());
        status.setChangeMessage("Retiring records");
        
        // --- use StatusActionsFactory and StatusActions class to
        // --- change status and carry out behaviours for status changes
        StatusActions sa = statusActionFactory.createStatusActions(srvContext);

        int author = srvContext.getUserSession().getUserIdAsInt();
        MetadataStatus metadataStatus = convertParameter(metadata.getId(), status, author);
        List<MetadataStatus> listOfStatusChange = new ArrayList<>(1);
        listOfStatusChange.add(metadataStatus);
        sa.onStatusChange(listOfStatusChange);

        Element md = metadata.getXmlData(false);
        
        Map<String, Object> xslParameters = new HashMap<String, Object>();
		xslParameters.put("publish_keyword", Geonet.Transform.RETIRED_INTERNAL);
		Path file = schemaManager.getSchemaDir(schema).resolve("process").resolve(Geonet.File.SET_KEYWORD);
		md = Xml.transform(md, file, xslParameters);
	
		// --- remove all privileges
		metadataOperations.deleteMetadataOper(String.valueOf(metadata.getId()), false);
		
		// --- update Metadata
		metadataManager.updateMetadata(srvContext, String.valueOf(metadata.getId()), md, false, false, false, srvContext.getLanguage(), new ISODate().toString(), false);
        
        // --- reindex metadata
        metadataIndexer.indexMetadata(String.valueOf(metadata.getId()), true, null);
        
        return true;
	}
	
	private void removeAssociations(AbstractMetadata metadata, String schema, ServiceContext srvContext) {
		/*
		 * If service record - get the operatesOn uuidref and remove the association from dataset
		 * If dataset record - get the all the associations and remove 
		 */
		
        String schemaIdentifier;
		try {
			schemaIdentifier = metadataSchemaUtils.getMetadataSchema(String.valueOf(metadata.getId()));
			SchemaPlugin instance = SchemaManager.getSchemaPlugin(schemaIdentifier);
	        AssociatedResourcesSchemaPlugin schemaPlugin = null;
	        if (instance instanceof AssociatedResourcesSchemaPlugin) {
	            schemaPlugin = (AssociatedResourcesSchemaPlugin) instance;
	        }
	        
	        if(schemaPlugin != null) {
	        	Set<String> listOfAssociatedResources = schemaPlugin.getAssociatedDatasetUUIDs(metadata.getXmlData(false));
	        	listOfAssociatedResources.stream().forEach(uuidref -> {
	        		
	        		Map<String, Object> xslAssocParameters = new HashMap<String, Object>();
	        		xslAssocParameters.put("type", "UUID");
	        		xslAssocParameters.put("code", metadata.getUuid());
	        		xslAssocParameters.put("forceRemove", "true");
	        		
	        		// --- update Metadata
	        		try {
	        			
	        			//Remove service from dataset (association)
	        			AbstractMetadata associatedMetadata = ApiUtils.getRecord(uuidref);
	        			Element association_md = associatedMetadata.getXmlData(false);
		         		Path file = schemaManager.getSchemaDir(schema).resolve("process").resolve(Geonet.File.ASSOCIATION_REMOVE);
		        		association_md = Xml.transform(association_md, file, xslAssocParameters);
						metadataManager.updateMetadata(srvContext, String.valueOf(associatedMetadata.getId()), association_md, false, false, false, srvContext.getLanguage(), new ISODate().toString(), false);
						metadataIndexer.indexMetadata(String.valueOf(associatedMetadata.getId()), true, null);
						

						
						//Remove dataset from service (operatesOn)
						file = schemaManager.getSchemaDir(schema).resolve("process").resolve(Geonet.File.SERVICES_REMOVE);
		        		Element md = metadata.getXmlData(false);
		        		Map<String, Object> xslParameters = new HashMap<String, Object>();
		        		xslParameters.put("uuidref", uuidref);
		         		md = Xml.transform(md, file, xslParameters);
		         		metadataManager.updateMetadata(srvContext, String.valueOf(metadata.getId()), md, false, false, false, srvContext.getLanguage(), new ISODate().toString(), false);
						
		         		
					} catch (Exception e) {
						
					}
	                
	        	});
	        }
		} catch (Exception e) {
			Log.error(Geonet.DATA_MANAGER, "Unable to remove associations");
		}
        
	}
	 /**
     * Convert request parameter to a metadata status.
     */
    public MetadataStatus convertParameter(int id, MetadataStatusParameter parameter, int author) throws Exception {
        StatusValue statusValue = statusValueRepository.findOne(parameter.getStatus());

        MetadataStatus metadataStatus = new MetadataStatus();

        MetadataStatusId mdStatusId = new MetadataStatusId().setStatusId(parameter.getStatus()).setMetadataId(id)
                .setChangeDate(new ISODate()).setUserId(author);

        metadataStatus.setId(mdStatusId);
        metadataStatus.setStatusValue(statusValue);

        if (parameter.getChangeMessage() != null) {
            metadataStatus.setChangeMessage(parameter.getChangeMessage());
        }
        if (StringUtils.isNotEmpty(parameter.getDueDate())) {
            metadataStatus.setDueDate(new ISODate(parameter.getDueDate()));
        }
        if (StringUtils.isNotEmpty(parameter.getCloseDate())) {
            metadataStatus.setCloseDate(new ISODate(parameter.getCloseDate()));
        }
        if (parameter.getOwner() != null) {
            metadataStatus.setOwner(parameter.getOwner());
        }
        return metadataStatus;
    }
        
    @RequestMapping(value = "/retire/status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Boolean doiCreateStatus(HttpServletRequest request) throws Exception {
		return (Boolean) request.getSession().getAttribute(RETIRE_STATUS);
	}

	@RequestMapping(value = "/retire/report", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Map<String, String> doiCreateReport(HttpServletRequest request) throws Exception {
		return (Map<String, String>) request.getSession().getAttribute(RETIRE_REPORT);
	}
}
