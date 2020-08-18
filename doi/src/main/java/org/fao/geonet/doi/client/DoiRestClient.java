package org.fao.geonet.doi.client;

import static org.fao.geonet.doi.client.DoiSettings.LOGGER_NAME;

import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.fao.geonet.ApplicationContextHolder;
import org.fao.geonet.utils.GeonetHttpRequestFactory;
import org.fao.geonet.utils.Log;

import com.google.common.io.CharStreams;

public class DoiRestClient {

    public static final String DOI_ENTITY = "DOI";
    public static final String ALL_DOI_ENTITY = "All DOI";
    public static final String DOI_METADATA_ENTITY = "DOI metadata";
    protected String serverUrl;
    protected String username;
    protected String password;

    private boolean testMode;

    protected GeonetHttpRequestFactory requestFactory;

    public DoiRestClient(String serverUrl, String username, String password) {
        this(serverUrl, username, password, true);
    }

    public DoiRestClient(String serverUrl, String username, String password, boolean testMode) {
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl : serverUrl + "/";
        this.username = username;
        this.password = password;
        this.testMode = testMode;

        requestFactory =
            ApplicationContextHolder.get().getBean(GeonetHttpRequestFactory.class);
    }
  
	/**
     * POST will mint new DOI if specified DOI doesn't exist.
     * This method will attempt to update URL if you specify existing DOI.
     * Standard domains and quota restrictions check will be performed.
     * Datacentre's doiQuotaUsed will be increased by 1. A new record in Datasets will be created.
     *
     * @param doi   The DOI prefix for the datacenter / identifier eg. 10.5072/TEST-1
     * @param url   The landing page
     * @throws DoiClientException
     */
	public void createDoi(String doi, String requestBody, String url) throws DoiClientException {
	        create(createUrl("dois"), requestBody, DOI_ENTITY);
	}
	
	protected String createUrl(String service) {
		 return this.serverUrl +
		            (this.serverUrl.endsWith("/") ? "" : "/") +
		            service;
	}
 
	public String retrieveDoi(String doi) throws DoiClientException {
		return retrieve(createUrl("dois/" + doi));
	}
	
	protected String retrieve(String url)
            throws DoiClientException {

		CloseableHttpClient client = HttpClients.createDefault();
		CloseableHttpResponse response = null;
        HttpGet getMethod = null;

        try {
            Log.debug(LOGGER_NAME, "   -- URL: " + url);

            getMethod = new HttpGet(url);

            ((HttpUriRequest) getMethod).addHeader( new BasicHeader("Content-Type",  "application/vnd.api+json;charset=UTF-8") );
            
            Log.debug(LOGGER_NAME, "DoiClient >> retrieve, username: " + username + ", password: " + password);
            
            UsernamePasswordCredentials creds = new UsernamePasswordCredentials(username, password);
            
            getMethod.addHeader(new BasicScheme().authenticate(creds, getMethod, null));
            
            response = client.execute(getMethod);
            
            int status = response.getStatusLine().getStatusCode();

            Log.debug(LOGGER_NAME, "   -- Request status code: " + status);

            if (status == HttpStatus.SC_OK) {
                return "Success";
            } else if (status == HttpStatus.SC_NO_CONTENT) {
                return null; // Not found
            } else if (status == HttpStatus.SC_NOT_FOUND) {
                return null; // Not found
            } else {
                Log.info(LOGGER_NAME, "Retrieve DOI metadata end -- Error: " + response.getStatusLine().getReasonPhrase());

                try(InputStreamReader reader = new InputStreamReader(response.getEntity().getContent())){
                	throw new DoiClientException( response.getStatusLine().getReasonPhrase() +
                            CharStreams.toString(reader));	
                }
                
            }

        } catch (Exception ex) {
            Log.error(LOGGER_NAME, "   -- Error (exception): " + ex.getMessage());
            throw new DoiClientException(ex.getMessage());

        } finally {
            if (getMethod != null) {
                getMethod.releaseConnection();
            }
            // Release the connection.
            IOUtils.closeQuietly(response);
        }
    }
	/**
     * See https://support.datacite.org/docs/mds-api-guide#section-register-metadata
	 * @throws IOException 
     */
    protected void create(String url, String body, String entity)
            throws DoiClientException {

    	
		CloseableHttpClient client = HttpClients.createDefault();
        HttpPost postMethod = null;
        CloseableHttpResponse response = null;
        
        try {
        	Log.debug(LOGGER_NAME, "   -- URL: " + url);

            postMethod = new HttpPost(url);

            ((HttpUriRequest) postMethod).addHeader( new BasicHeader("Content-Type",  "application/vnd.api+json;charset=UTF-8") );

            Log.debug(LOGGER_NAME, "  ----- 1 -----");
            StringEntity requestEntity = new StringEntity(body, ContentType.APPLICATION_JSON);

            postMethod.setEntity(requestEntity);
            
            Log.debug(LOGGER_NAME, "DoiClient >> create, username: " + username + ", password: " + password);
            UsernamePasswordCredentials creds = new UsernamePasswordCredentials(username, password);
            
            postMethod.addHeader(new BasicScheme().authenticate(creds, postMethod, null));
          
            Log.debug(LOGGER_NAME, "  ----- 3 -----");
            response = client.execute(postMethod);
            
            Log.debug(LOGGER_NAME, " --- response " + response.getStatusLine().getReasonPhrase());

            int status = response.getStatusLine().getStatusCode();

            Log.debug(LOGGER_NAME, " status: " + status);
           
            if (status != HttpStatus.SC_CREATED) {
                String message = String.format(
                    "Failed to create '%s'. Status is %d. Error is %s.",
                    url, status,
                    response.getStatusLine().getReasonPhrase());
                
                Log.info(LOGGER_NAME, message);

                throw new DoiClientException(message);
            } else {
            	Log.debug(LOGGER_NAME, String.format(
                    "DOI metadata created at %s.", url));
            }
        } catch (Exception ex) {
        	Log.error(LOGGER_NAME, "   -- Error (exception): " + ex.getMessage());
            throw new DoiClientException(ex.getMessage());

        } finally {
            if (postMethod != null) {
                postMethod.releaseConnection();
            }
            
         // Release the connection.
            IOUtils.closeQuietly(client);
            IOUtils.closeQuietly(response);
        }
        
    }
    
    
    /**
     * See https://support.datacite.org/docs/mds-api-guide#section-register-metadata
	 * @throws IOException 
     */
    protected void update(String url, String body)
            throws DoiClientException {

    	
		CloseableHttpClient client = HttpClients.createDefault();
        HttpPut putMethod = null;
        CloseableHttpResponse response = null;
        
        try {
        	Log.debug(LOGGER_NAME, " DoiRestClient, update  -- URL: " + url);
        	Log.debug(LOGGER_NAME, " DoiRestClient, update  -- Body: " + body);

        	putMethod = new HttpPut(url);

            ((HttpUriRequest) putMethod).addHeader( new BasicHeader("Content-Type",  "application/vnd.api+json;charset=UTF-8") );

            Log.debug(LOGGER_NAME, "  ----- 1 -----");
            StringEntity requestEntity = new StringEntity(body, ContentType.APPLICATION_JSON);

            putMethod.setEntity(requestEntity);
            
            Log.debug(LOGGER_NAME, "DoiClient >> create, username: " + username + ", password: " + password);
            UsernamePasswordCredentials creds = new UsernamePasswordCredentials(username, password);
            
            putMethod.addHeader(new BasicScheme().authenticate(creds, putMethod, null));
          
            Log.debug(LOGGER_NAME, "  ----- 3 -----");
            response = client.execute(putMethod);
            
            Log.debug(LOGGER_NAME, " --- response " + response.getStatusLine().getReasonPhrase());

            int status = response.getStatusLine().getStatusCode();

            Log.debug(LOGGER_NAME, " status: " + status);
           
            if (status != HttpStatus.SC_CREATED) {
                String message = String.format(
                    "Failed to create '%s'. Status is %d. Error is %s.",
                    url, status,
                    response.getStatusLine().getStatusCode());
                
                Log.info(LOGGER_NAME, message);

                throw new DoiClientException(message);
            } else {
            	Log.debug(LOGGER_NAME, String.format(
                    "DOI metadata created at %s.", url));
            }
        } catch (Exception ex) {
        	Log.error(LOGGER_NAME, "   -- Error (exception): " + ex.getMessage());
            throw new DoiClientException(ex.getMessage());

        } finally {
            if (putMethod != null) {
                putMethod.releaseConnection();
            }
            
         // Release the connection.
            IOUtils.closeQuietly(client);
            IOUtils.closeQuietly(response);
        }
        
    }
}
