/*
 * Copyright (C) 2001-2016 Food and Agriculture Organization of the
 * United Nations (FAO-UN), United Nations World Food Programme (WFP)
 * and United Nations Environment Programme (UNEP)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
 *
 * Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
 * Rome - Italy. email: geonetwork@osgeo.org
 */
package org.fao.geonet.api.records;

import static org.fao.geonet.api.ApiParams.API_CLASS_RECORD_TAG;
import static org.fao.geonet.api.ApiParams.API_PARAM_RECORD_ECATID;

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.fao.geonet.ApplicationContextHolder;
import org.fao.geonet.api.API;
import org.fao.geonet.api.ApiParams;
import org.fao.geonet.api.ApiUtils;
import org.fao.geonet.api.exception.ResourceNotFoundException;
import org.fao.geonet.constants.Edit;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.doi.client.DoiManager;
import org.fao.geonet.doi.client.DoiRestManager;
import org.fao.geonet.doi.client.DoiSettings;
import org.fao.geonet.domain.AbstractMetadata;
import org.fao.geonet.domain.Constants;
import org.fao.geonet.domain.Metadata;
import org.fao.geonet.domain.MetadataDataInfo;
import org.fao.geonet.kernel.ECatOperationManager;
import org.fao.geonet.kernel.SelectionManager;
import org.fao.geonet.kernel.datamanager.base.BaseMetadataStatus;
import org.fao.geonet.kernel.setting.SettingManager;
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
import org.springframework.web.bind.annotation.PathVariable;
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

/**
 * Handle DOI creation.
 */
@RequestMapping(value = {
    "/{portal}/api/records",
    "/{portal}/api/" + API.VERSION_0_1 +
        "/records"
})
@Api(value = API_CLASS_RECORD_TAG,
    tags = API_CLASS_RECORD_TAG)
@Controller("doi")
@PreAuthorize("hasRole('Editor')")
@ReadWriteController
public class DoiApi {

//    @Autowired
//    private DoiManager doiManager;
    
	@Autowired
	private DoiRestManager doiRestManager;
	
    @Autowired
    ECatOperationManager opManager;
    
    @Autowired
	ApplicationContext context;
    
    @Autowired
    BaseMetadataStatus metadataStatus;
    
    private Map<String, String> report = new HashMap<>();
    
    static final String DOI_REPORT = "doi_report";
    static final String DOI_CREATE_STATUS = "doi_create_status";
    
    @ApiOperation(
        value = "Check that a record can be submitted to DataCite for DOI creation. " +
            "DataCite requires some fields to be populated.",
        nickname = "checkDoiStatus")
    @RequestMapping(value = "/{eCatId}/doi/checkPreConditions",
        method = RequestMethod.GET,
        produces = {
            MediaType.APPLICATION_JSON_VALUE
        }
    )
    @PreAuthorize("hasRole('Administrator')")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Record can be proposed to DataCite."),
        @ApiResponse(code = 404, message = "Metadata not found."),
        @ApiResponse(code = 400, message = "Record does not meet preconditions. Check error message."),
        @ApiResponse(code = 500, message = "Service unavailable."),
        @ApiResponse(code = 403, message = ApiParams.API_RESPONSE_NOT_ALLOWED_CAN_EDIT)
    })
    public
    @ResponseBody
    ResponseEntity<Map<String, Boolean>> checkDoiStatus(
        @ApiParam(
            value = API_PARAM_RECORD_ECATID,
            required = true)
        @PathVariable
            String eCatId,
        @ApiParam(hidden = true)
        @ApiIgnore
            HttpServletRequest request,
        @ApiParam(hidden = true)
        @ApiIgnore
            HttpSession session
    ) throws Exception {
    	
    	ServiceContext serviceContext = ApiUtils.createServiceContext(request);
    	Metadata md = getMetadata(serviceContext, eCatId);
    	
        AbstractMetadata metadata = ApiUtils.canEditRecord(md.getUuid(), request);
        
        //DoiManager doiManager = getDoiManager(session);
        final Map<String, Boolean> reportStatus = doiRestManager.check(serviceContext, metadata, null, eCatId);
        return new ResponseEntity<>(reportStatus, HttpStatus.OK);
    }


    @ApiOperation(
        value = "Submit a record to the Datacite metadata store in order to create a DOI.",
        nickname = "createDoi")
    @RequestMapping(value = "/{eCatId}/doi",
        method = RequestMethod.POST,
        produces = {
            MediaType.APPLICATION_JSON_VALUE
        }
    )
    @PreAuthorize("hasRole('Administrator')")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Check status of the report."),
        @ApiResponse(code = 404, message = "Metadata not found."),
        @ApiResponse(code = 500, message = "Service unavailable."),
        @ApiResponse(code = 403, message = ApiParams.API_RESPONSE_NOT_ALLOWED_CAN_EDIT)
    })
    public
    @ResponseBody
    ResponseEntity<Map<String, String>> createDoi(
        @ApiParam(
            value = API_PARAM_RECORD_ECATID,
            required = true)
        @PathVariable
            String eCatId,
        @ApiParam(hidden = true)
        @ApiIgnore
            HttpServletRequest request,
        @ApiParam(hidden = true)
        @ApiIgnore
            HttpSession session
    ) throws Exception {
    	
    	//DoiManager doiManager = getDoiManager(session);
    
        ServiceContext serviceContext = ApiUtils.createServiceContext(request);
        Metadata md = getMetadata(serviceContext, eCatId);
        AbstractMetadata metadata = ApiUtils.canEditRecord(md.getUuid(), request);
        Map<String, String> doiInfo = doiRestManager.register(serviceContext, metadata, eCatId);
        return new ResponseEntity<>(doiInfo, HttpStatus.CREATED);
    }
    
    @ApiOperation(
            value = "Submit a record to the Datacite metadata store in order to create a DOI.",
            nickname = "updateDoi")
        @RequestMapping(value = "/{eCatId}/doi/{doi}",
            method = RequestMethod.PUT,
            produces = {
                MediaType.APPLICATION_JSON_VALUE
            }
        )
        @PreAuthorize("hasRole('Administrator')")
        @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Check status of the report."),
            @ApiResponse(code = 404, message = "Metadata not found."),
            @ApiResponse(code = 500, message = "Service unavailable."),
            @ApiResponse(code = 403, message = ApiParams.API_RESPONSE_NOT_ALLOWED_CAN_EDIT)
        })
        public
        @ResponseBody
        ResponseEntity<Map<String, String>> updateDoi(
        		@ApiParam(value = API_PARAM_RECORD_ECATID,required = true) @PathVariable String eCatId,
        		@ApiParam(value = "doi",required = true) @PathVariable String doi,
        		@ApiParam(hidden = true) @ApiIgnore HttpServletRequest request,
        		@ApiParam(hidden = true) @ApiIgnore HttpSession session
        ) throws Exception {
        	
        	//DoiManager doiManager = getDoiManager(session);
        
            ServiceContext serviceContext = ApiUtils.createServiceContext(request);
            Metadata md = getMetadata(serviceContext, eCatId);
            AbstractMetadata metadata = ApiUtils.canEditRecord(md.getUuid(), request);
            Map<String, String> doiInfo = doiRestManager.update(serviceContext, metadata, eCatId, doi);
            return new ResponseEntity<>(doiInfo, HttpStatus.NO_CONTENT);
        }

//    Do not provide support for DOI removal ?
    @ApiOperation(
        value = "Remove a DOI (this is not recommended, DOI are supposed to be persistent once created. This is mainly here for testing).",
        nickname = "deleteDoi")
    @RequestMapping(value = "/{eCatId}/doi",
        method = RequestMethod.DELETE,
        produces = {
            MediaType.APPLICATION_JSON_VALUE
        }
    )
    @PreAuthorize("hasRole('Administrator')")
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "DOI unregistered."),
        @ApiResponse(code = 404, message = "Metadata or DOI not found."),
        @ApiResponse(code = 500, message = "Service unavailable."),
        @ApiResponse(code = 403, message = ApiParams.API_RESPONSE_NOT_ALLOWED_ONLY_ADMIN)
    })
    public ResponseEntity deleteDoi(@ApiParam(value = API_PARAM_RECORD_ECATID, required = true) @PathVariable String eCatId, 
        @ApiParam(hidden = true) @ApiIgnore HttpServletRequest request, 
        @ApiParam(hidden = true) @ApiIgnore HttpSession session
    ) throws Exception {
    	ServiceContext serviceContext = ApiUtils.createServiceContext(request);
    	Metadata md = getMetadata(serviceContext, eCatId);
        AbstractMetadata metadata = ApiUtils.canEditRecord(md.getUuid(), request);
        DoiManager doiManager = context.getBean(DoiManager.class);
        doiManager.unregisterDoi(metadata, serviceContext, eCatId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = "Create DOI in bulk.", nickname = "createBulkDoi")
    @RequestMapping(value = "/doi/bulk", method = RequestMethod.PUT)
    @PreAuthorize("hasRole('Administrator')")
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "DOI created."),
        @ApiResponse(code = 404, message = "Metadata or DOI not found."),
        @ApiResponse(code = 500, message = "Service unavailable."),
        @ApiResponse(code = 403, message = ApiParams.API_RESPONSE_NOT_ALLOWED_ONLY_ADMIN)
    })
    public ResponseEntity createDOI(@RequestParam(required = false) String bucket, HttpServletRequest request, @ApiParam(hidden = true) @ApiIgnore HttpSession session) {
    	
    	if(report != null && !report.isEmpty())
    		report.clear();
    	
    	request.getSession().setAttribute(DOI_CREATE_STATUS, false);
    	ServiceContext context = ApiUtils.createServiceContext(request);
    	Runnable task = () -> {
    		startDOICreation(context, bucket, session);
		};

		// start the thread
		new Thread(task).start();
		
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    
    private void startDOICreation(ServiceContext serviceContext, String bucket, HttpSession session) {

    	//final DoiManager doiManager = getDoiManager(session);
		
		try {
			
			UserSession userSession = serviceContext.getUserSession();
			SelectionManager sm = SelectionManager.getManager(userSession);

			Set<String> uuids = new HashSet<>();
			if (sm != null) {

				synchronized (sm.getSelection(bucket)) {
					for (Iterator<String> iter = sm.getSelection(bucket).iterator(); iter.hasNext();) {
						String uuid = (String) iter.next();
						Log.debug("DOI", "DoiApi, SelectionBucket >>  --> UUID: " + uuid);
						uuids.add(uuid);
					}
				}

			}
			
			uuids.stream().forEach(uuid -> {
				Log.debug("DOI", "DoiApi >>  --> UUID: " + uuid);
				String eCatId = "";
				try {
					
					AbstractMetadata metadata = ApiUtils.getRecord(uuid);
					if(metadata.getDataInfo().getType().codeString.equals(String.valueOf(Constants.YN_FALSE))) {
						eCatId = opManager.getECatIdFromUUID(context, uuid);
						Log.debug("DOI", "DoiApi >>  --> CurrentStatus: " + metadataStatus.getCurrentStatus(metadata.getId()));
						
						if(Integer.parseInt(metadataStatus.getCurrentStatus(metadata.getId())) == Geonet.WorkflowStatus.APPROVED) {
					        Map<String, String> doiInfo = doiRestManager.register(serviceContext, metadata, eCatId);
					        report.put(eCatId, "Successfully created DOI for the eCatId: " + eCatId);
						}else {
							report.put(eCatId, "This record is not approved. Required to publish or must be in approved state in order to create DOI.");
						}
					}else {
						report.put(eCatId, "Not a metadata record. Selected record seems to be Template/Sub directory.");
					}					
				}catch(Exception rnfe) {
					Log.debug("DOI", "DoiApi >>  --> rnfe.getMessage(): " + rnfe.getMessage());
					StackTraceElement[] eles = rnfe.getStackTrace();
					for (StackTraceElement ste : eles) {
						Log.debug("DOI", "DoiApi >>  --> Error at " + ste.getClassName() + ", " + ste.getMethodName() + " - " + ste.getLineNumber());
					}
					report.put(eCatId, rnfe.getMessage());
				}
			});
			
		} catch (Exception e) {
			Log.error(Geonet.SCHEMA_MANAGER, " Bulk DOI creation failed, Error is " + e.getMessage());
		} finally {
			session.setAttribute(DOI_REPORT, report);
			session.setAttribute(DOI_CREATE_STATUS, true);
		}
		
	}

	@RequestMapping(value = "/doi/status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Boolean doiCreateStatus(HttpServletRequest request) throws Exception {
		return (Boolean) request.getSession().getAttribute(DOI_CREATE_STATUS);
	}
	
	@RequestMapping(value = "/doi/report", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Map<String, String> doiCreateReport(HttpServletRequest request) throws Exception {
		return (Map<String, String>) request.getSession().getAttribute(DOI_REPORT);
	}
    
    private Metadata getMetadata(ServiceContext serviceContext, String eCatId) throws Exception {
    	
    	Metadata record = opManager.getMetadataFromECatId(serviceContext.getApplicationContext(), eCatId);
    	return record;
    }
    
	/*
	 * private DoiManager getDoiManager(HttpSession session) {
	 * 
	 * DoiManager doiManager; SettingManager setmanager =
	 * ApplicationContextHolder.get().getBean(SettingManager.class); String
	 * serverUrl = setmanager.getValue(DoiSettings.SETTING_PUBLICATION_DOI_DOIURL);
	 * 
	 * Log.debug("DOI", "   -- DoiApi >> serverUrl: " + serverUrl);
	 * if(serverUrl.startsWith("https://api")) { doiManager =
	 * context.getBean("doiRestManager", DoiRestManager.class); }else { doiManager =
	 * context.getBean("doiManager", DoiManager.class); }
	 * 
	 * return doiManager; }
	 */

//    TODO: At some point we may add support for DOI States management
//    https://support.datacite.org/docs/mds-api-guide#section-doi-states
}
