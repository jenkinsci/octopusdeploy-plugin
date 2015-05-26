package com.octopusdeploy.api;

import java.io.*;
import java.net.*;
import net.sf.json.*;

/**
 *
 */
public class AuthenticatedWebClient {
    
    public static final String GET = "GET";
    public static final String POST = "POST";
    private static final String OCTOPUS_API_KEY_HEADER = "X-Octopus-ApiKey";
    
    private final String hostUrl;
    private final String apiKey;
    
    public AuthenticatedWebClient(String hostUrl, String apiKey) {
        this.hostUrl = hostUrl;
        this.apiKey = apiKey;
    }
    
    public JSON MakeRequest(String method, String endpoint)
            throws IllegalArgumentException, IOException, MalformedURLException, ProtocolException {
        return MakeRequest(method, endpoint, null);
    }
    
    public JSON MakeRequest(String method, String endpoint, String queryParameters) 
            throws IllegalArgumentException, IOException, MalformedURLException, ProtocolException {
        if (!GET.equals(method) && !POST.equals(method)) {
            throw new IllegalArgumentException ("Invalid method supplied.");
        }
        
        String joinedUrl = String.join("/", hostUrl, endpoint);
        if (GET.equals(method) && queryParameters != null && !queryParameters.isEmpty())
        {
            joinedUrl = String.join("?", joinedUrl, queryParameters);
        }
        URL url = new URL(joinedUrl);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty(OCTOPUS_API_KEY_HEADER, apiKey);
        
        if (POST.equals(method) && queryParameters != null && !queryParameters.isEmpty()) {
            connection.setDoOutput(true);
            connection.connect();
            DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
            dataOutputStream.writeBytes(queryParameters);
            dataOutputStream.flush();
            dataOutputStream.close();
        }
        connection.connect();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = reader.readLine()) != null) {
            response.append(inputLine);
        }
        reader.close();
        connection.disconnect();
        return JSONSerializer.toJSON(response.toString());
    }
}
