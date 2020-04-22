package org.fao.geonet.api.records;

import static org.fao.geonet.api.ApiParams.API_CLASS_RECORD_OPS;
import static org.fao.geonet.api.ApiParams.API_CLASS_RECORD_TAG;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.transform.stream.StreamResult;

import org.fao.geonet.api.API;
import org.fao.geonet.api.ApiParams;
import org.fao.geonet.api.ApiUtils;
import org.fao.geonet.constants.Edit;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.domain.Metadata;
import org.fao.geonet.domain.MetadataDataInfo;
import org.fao.geonet.kernel.GeonetworkDataDirectory;
import org.fao.geonet.kernel.SelectionManager;
import org.fao.geonet.repository.MetadataRepository;
import org.fao.geonet.utils.Log;
import org.fao.geonet.utils.Xml;
import org.jdom.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import jeeves.server.UserSession;
import jeeves.server.context.ServiceContext;

@RequestMapping(value = { "/{portal}/api/records", "/{portal}/api/" + API.VERSION_0_1 + "/records" })
@Api(value = API_CLASS_RECORD_TAG, tags = API_CLASS_RECORD_TAG, description = API_CLASS_RECORD_OPS)
@PreAuthorize("permitAll")
@RestController
public class MetadataDownloadApi {

	private ApplicationContext appContext;

	@Autowired
    private GeonetworkDataDirectory dataDirectory;
	
	@Autowired
    private MetadataRepository metadataRepository;
	
	
	public synchronized void setApplicationContext(ApplicationContext context) {
		this.appContext = context;
	}

	@RequestMapping(value = "/download/csv", method = RequestMethod.PUT, produces=MediaType.TEXT_PLAIN_VALUE)
	@ResponseBody
	public String downloadCSV(
			@ApiParam(value = ApiParams.API_PARAM_BUCKET_NAME, required = false) @RequestParam(required = false) String bucket,
			@RequestParam String[] exportParams, HttpServletRequest request) throws Exception {

		ServiceContext context = ApiUtils.createServiceContext(request);

		Map<String, Object> mapParams = new HashMap<String, Object>();
		for (String param : exportParams) {
			mapParams.put(param, true);
		}
		String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();
		// return prepareCsv(context, appContext, bucket, mapParams);
		Path csvPath = Files.createTempFile(sessionId + "metadatas", ".csv");
		Runnable task = () -> {
			prepareCsv(context, appContext, bucket, mapParams, csvPath, request.getSession());
		};

		// start the thread
		new Thread(task).start();
		
		return csvPath.toFile().getName();

	}

	public void prepareCsv(ServiceContext srvContext, ApplicationContext context, String bucket,
			Map<String, Object> exportParams, Path csvPath, HttpSession session) {

		boolean isDone = false;
		session.setAttribute(Geonet.CSV_DOWNLOAD_STATUS, isDone);
		/*exportParams.entrySet().iterator().forEachRemaining(ep -> {
			Log.debug(Geonet.SEARCH_ENGINE, "export params values --> " + ep.getKey() + ": " + ep.getValue());
		});*/

		try {
			
			UserSession userSession = srvContext.getUserSession();
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
			Log.debug(Geonet.SEARCH_ENGINE,
					"CSV MetadataDownloadApi --> Time pit1: " + new Date(System.currentTimeMillis()).toString());

			Element response = new Element("response");

			uuids.stream().forEach(uuid -> {
				try {
					Metadata _metadata = metadataRepository.findOneByUuid(uuid);
					final MetadataDataInfo dataInfo = _metadata.getDataInfo();

					Element info = new Element(Edit.RootChild.INFO, Edit.NAMESPACE);
					info.addContent(new Element(Edit.Info.Elem.ID).setText(String.valueOf(_metadata.getId())));
					info.addContent(new Element(Edit.Info.Elem.SCHEMA).setText(dataInfo.getSchemaId()));
					info.addContent(new Element(Edit.Info.Elem.UUID).setText(_metadata.getUuid()));

					Element md = _metadata.getXmlData(false).addContent(info);
					response.addContent(md);

				} catch (Exception e) {
					Log.error(Geonet.SEARCH_ENGINE, "Exception while getting metadta...");
				}

			});

			Log.debug(Geonet.SEARCH_ENGINE,
					"CSV MetadataDownloadApi --> Time pit2: " + new Date(System.currentTimeMillis()).toString());
			Element root = Xml.loadString("<root></root>", false);

			root.addContent(response);

			Path xslProcessing = dataDirectory.getWebappDir().resolve("xslt/services/csv").resolve("csv-search.xsl");

			csvPath.toFile().deleteOnExit();

			Log.debug(Geonet.SEARCH_ENGINE,
					"XslProcessing stylesheet path: " + xslProcessing.toFile().getAbsolutePath());

			try (OutputStream os = Files.newOutputStream(csvPath)) {
				StreamResult resStream = new StreamResult(os);
				Xml.transform(root, xslProcessing, resStream, exportParams);

			} catch (FileNotFoundException e) {
				Log.error(Geonet.SCHEMA_MANAGER, "Download file not found " + csvPath + ". Error is " + e.getMessage());
			}
		} catch (Exception e) {
			Log.error(Geonet.SCHEMA_MANAGER, "     Download csv compilation failed, Error is " + e.getMessage());
		} finally {
			isDone = true;
			session.setAttribute(Geonet.CSV_DOWNLOAD_STATUS, isDone);
		}
		
	}

	@RequestMapping(value = "/download/status", method = RequestMethod.GET)
	public boolean downloadCSVStatus(HttpServletRequest request) throws Exception {
		return (boolean) request.getSession().getAttribute(Geonet.CSV_DOWNLOAD_STATUS);
		
	}

	@RequestMapping(value = "/download/csv", method = RequestMethod.GET, produces = "text/csv")
	public ResponseEntity<Object> downloadCSV(@RequestParam String filename, HttpServletRequest request) throws Exception {

		try {
			String tempDir = System.getProperty("java.io.tmpdir");
			Path csvPath = Paths.get(tempDir + File.separator +filename);
			String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();
			
			if(!filename.contains(sessionId)){
				return new ResponseEntity<>("Not authorised to download", HttpStatus.UNAUTHORIZED); 
			}
			Log.debug(Geonet.SEARCH_ENGINE,
					"CSV MetadataDownloadApi, downloadCSV --> file.getAbsolutePath(): " + csvPath);
			
			HttpHeaders headers = new HttpHeaders();
			headers.add("Content-disposition", String.format("attachment;filename=%s", "metadata_records.csv"));
			InputStreamResource resource = new InputStreamResource(new FileInputStream(csvPath.toFile()));
			ResponseEntity<Object> responseEntity = ResponseEntity.ok().headers(headers).contentLength(csvPath.toFile().length())
					.body(resource);

			return responseEntity;

		} catch (FileNotFoundException e) {
			Log.error(Geonet.SCHEMA_MANAGER, "     Download file not found " + ". Error is " + e.getMessage());
		} catch (Exception e) {
			Log.error(Geonet.SCHEMA_MANAGER,
					"     Download csv compilation failed for " + ". Error is " + e.getMessage());
		}

		return new ResponseEntity<>("Unable to download csv", HttpStatus.INTERNAL_SERVER_ERROR);

	}

}
