package com.octopusdeploy.api;

import java.io.*;
import java.net.*;
import java.util.*;
import net.sf.json.*;

/**
 * An Octopus Deploy web API client that automatically puts the API key in a header
 * Offers GET and POST, returning the response as JSON.
 */
public class AuthenticatedWebClient {
    
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
     * @throws ProtocolException
     * @throws IOException 
     */
    public JSON post(String resource, byte[] data) throws ProtocolException, IOException
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
    public JSON get(String resource) throws IOException {
        return get(resource, null);
    }
    
    /**
     * Executes a get request against the resource provided.
     * @param resource the URL to the resource (omitting the host portion)
     * @param queryParameters a map of keys and values to include in the get. 
     * @return JSON blob representing the response from the server.
     * @throws IOException 
     */
    public JSON get(String resource, Map<String, String> queryParameters) throws IOException {
        String encodedParameterString = mapToQueryParameters(queryParameters);
        URLConnection connection = getConnection(GET, resource, encodedParameterString);
        return getResponse(connection);
    }
    
    /**
     * Returns a string that represents the query parameter component of the URL string.
     * Encodes all values using UTF-8 URL encoding.
     * @param queryParametersMap a map of keys and values for query parameters
     * @return a string of form "key1=value1&key2=value2"
     * @throws UnsupportedEncodingException 
     */
    private String mapToQueryParameters(Map<String, String> queryParametersMap) throws UnsupportedEncodingException {
        Set<String> parameterKeyValuePairs = new HashSet<String>();
        if (queryParametersMap != null && !queryParametersMap.isEmpty()) {
            for (Map.Entry<String, String> entry : queryParametersMap.entrySet()) {
                String encodedValue = URLEncoder.encode(entry.getValue(), "UTF-8");
                String kvp = String.format("%s=%s", entry.getKey(), encodedValue);
                parameterKeyValuePairs.add(kvp);
            }
        }
        return String.join("&", parameterKeyValuePairs);
    }
    
    /**
     * Creates and returns a new URLConnection object using the given information.
     * @param method GET or POST
     * @param endpoint the resource endpoint to connect to
     * @param queryParameters query parameters string to use in GET requests
     * @return the URLConnection (may be HTTP or HTTPS)
     * @throws MalformedURLException
     * @throws ProtocolException
     * @throws IOException 
     */
    private URLConnection getConnection(String method, String endpoint, String queryParameters) 
        throws MalformedURLException, ProtocolException, IOException {
        if (!GET.equals(method) && !POST.equals(method)) {
            throw new IllegalArgumentException(String.format("Unsupported method '%s'.", method));
        }

        String joinedUrl = String.join("/", hostUrl, endpoint);
        if (GET.equals(method) && queryParameters != null && !queryParameters.isEmpty())
        {
            joinedUrl = String.join("?", joinedUrl, queryParameters);
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
     * @throws IOException 
     */
    private JSON getResponse(URLConnection connection) throws IOException  {
        if (connection == null)
        {
            throw new IllegalArgumentException("Connection can not be null when getting a response from server.");
        }
        connection.connect();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = reader.readLine()) != null) {
            response.append(inputLine);
        }
        reader.close();
        if (connection instanceof HttpURLConnection) {
            ((HttpURLConnection)connection).disconnect();
        }
        return JSONSerializer.toJSON(response.toString());
    }
}
