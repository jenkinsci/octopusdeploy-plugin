package com.octopusdeploy.api;

import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.commons.lang.StringUtils;

/**
 * An Octopus Deploy web API client that automatically puts the API key in a header
 * Offers GET and POST, returning the response as JSON.
 */
public class AuthenticatedWebClient {
    private static final String UTF8 = "UTF-8";
    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String OCTOPUS_API_KEY_HEADER = "X-Octopus-ApiKey";
    
    private final String hostUrl;
    private final String apiKey;
    
    /**
     * Create a new instance.
     * @param hostUrl URL to the Octopus Deploy host. example: https://octopus.company.com/
     * @param apiKey The Octopus Deploy API key to use in making API requests
     */
    public AuthenticatedWebClient(String hostUrl, String apiKey) {
        this.hostUrl = hostUrl;
        this.apiKey = apiKey;
    }
    
    /**
     * Executes a post against the resource provided.
     * Uses content type application/x-www-form-urlencoded
     * @param resource the URL to the resource (omitting the host portion)
     * @param data an encoded data array of the data to post
     * @return JSON blob representing the response from the server.
     * @throws ProtocolException if the operation is performed on a URL that is not HTTP or HTTPS
     * @throws IOException 
     * @throws IllegalArgumentException When data to post is null
     */
    public WebResponse post(String resource, byte[] data) throws ProtocolException, IOException
    {
        if (data == null)
        {
            throw new IllegalArgumentException("Data to post can not be null");
        }
        URLConnection connection = getConnection(POST, resource, null);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");        
        connection.setRequestProperty("Content-Length", Integer.toString(data.length));
        connection.setDoOutput(true);
        connection.connect();
        DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
        dataOutputStream.write(data);
        dataOutputStream.flush();
        dataOutputStream.close();
        return getResponse(connection);
    }
    
    /**
     * Executes a get request against the resource provided.
     * @param resource the URL to the resource (omitting the host portion)
     * @return JSON blob representing the response from the server.
     * @throws IOException 
     */
    public WebResponse get(String resource) throws IOException {
        return get(resource, null);
    }
    
    /**
     * Executes a get request against the resource provided.
     * @param resource the URL to the resource (omitting the host portion)
     * @param queryParameters a map of keys and values to include in the get. 
     * @return JSON blob representing the response from the server.
     * @throws IOException 
     */
    public WebResponse get(String resource, Map<String, String> queryParameters) throws IOException {
        String encodedParameterString = mapToQueryParameters(queryParameters);
        URLConnection connection = getConnection(GET, resource, encodedParameterString);
        return getResponse(connection);
    }
    
    /**
     * Returns a string that represents the query parameter component of the URL string.
     * Encodes all values using UTF-8 URL encoding.
     * @param queryParametersMap a map of keys and values for query parameters
     * @return a string of form "key1=value1&key2=value2"
     * @throws UnsupportedEncodingException when/if the url encoding to UTF8 fails.
     */
    private String mapToQueryParameters(Map<String, String> queryParametersMap) throws UnsupportedEncodingException {
        Set<String> parameterKeyValuePairs = new HashSet<String>();
        if (queryParametersMap != null && !queryParametersMap.isEmpty()) {
            for (Map.Entry<String, String> entry : queryParametersMap.entrySet()) {
                String encodedValue = URLEncoder.encode(entry.getValue(), UTF8);
                String kvp = String.format("%s=%s", entry.getKey(), encodedValue);
                parameterKeyValuePairs.add(kvp);
            }
        }
        return StringUtils.join(parameterKeyValuePairs, "&");
    }
    
    /**
     * Creates and returns a new URLConnection object using the given information.
     * @param method GET or POST
     * @param endpoint the resource endpoint to connect to
     * @param queryParameters query parameters string to use in GET requests
     * @return the URLConnection (may be HTTP or HTTPS)
     * @throws MalformedURLException if the supplied url is not a valid url
     * @throws ProtocolException if the supplied url is not http or https
     * @throws IOException if there is a failure establishing an http connection
     * @throws IllegalArgumentException if the provided method is not GET or POST
     */
    private URLConnection getConnection(String method, String endpoint, String queryParameters) 
        throws MalformedURLException, ProtocolException, IOException, IllegalArgumentException {
        if (!GET.equals(method) && !POST.equals(method)) {
            throw new IllegalArgumentException(String.format("Unsupported method '%s'.", method));
        }

        String joinedUrl = StringUtils.join(new String[] {hostUrl, endpoint}, "/");
        if (GET.equals(method) && queryParameters != null && !queryParameters.isEmpty())
        {
            joinedUrl = StringUtils.join(new String[]{joinedUrl, queryParameters}, "?");
        }
        URL url = new URL(joinedUrl);
        URLConnection connection = url.openConnection();
        if (connection instanceof HttpURLConnection) {
            ((HttpURLConnection)connection).setRequestMethod(method);
        }
        connection.setRequestProperty(OCTOPUS_API_KEY_HEADER, apiKey);
        return connection;
    }
    
    /**
     * Use the connection to read a response from the server.
     * @param connection an instantiated URLConnection object.
     * @return JSON blob representing the response from the server.
     * @throws IOException if there is an issue when connecting or reading the response
     * @throws IllegalArgumentException if the connection is null
     */
    private WebResponse getResponse(URLConnection connection) throws IOException, IllegalArgumentException  {
        int responseCode = -1;
        if (connection == null)
        {
            throw new IllegalArgumentException("Connection can not be null when getting a response from server.");
        }
        connection.connect();
        InputStream streamToRead = null;
        if(connection instanceof HttpURLConnection) {
            responseCode = ((HttpURLConnection)connection).getResponseCode();
            if (isErrorCode(responseCode))
            {
                streamToRead = ((HttpURLConnection)connection).getErrorStream();
            }
        }
        if (streamToRead == null) {
            streamToRead = connection.getInputStream();
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(streamToRead));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = reader.readLine()) != null) {
            response.append(inputLine);
        }
        reader.close();
        if (connection instanceof HttpURLConnection) {
            ((HttpURLConnection)connection).disconnect();
        }
        return new WebResponse(responseCode, response.toString());
    }
    
   
    /**
    * Returns true if the HTTP Response code represents an error.
    * @param code the HTTP Response code
    * @return true or false
    */
   public final static boolean isErrorCode(final int code) {
       return code >= 400;
   }
    
    /**
     * A web response code (HTTP Response code) and content from the web request.
     */
    public class WebResponse {
        private final int code;
        /**
         * The HTTP response code.
         * @return The HTTP response code. Ex. 200 or 403
         */
        public int getCode() {
            return code;
        }
        
        /**
         * Returns true if the HTTP Response code represents an error.
         * @return true or false
         */
        public boolean isErrorCode() {
            return AuthenticatedWebClient.isErrorCode(code);
        }
        
        private final String content;
        /**
         * Content for the web response, if any.
         * @return JSON content for the web response, if any.
         */
        public String getContent() {
            return content;
        }
        
        private WebResponse(int code, String content) {
            this.code = code;
            this.content = content;
        }   
    }
}
