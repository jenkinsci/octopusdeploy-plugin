package com.octopusdeploy.api;

import com.octopusdeploy.api.data.Channel;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/**
 * Methods for the Channels aspect of the Octopus API
 */
public class ChannelsApi {
    private final static String UTF8 = "UTF-8";
    private final AuthenticatedWebClient webClient;

    public ChannelsApi(AuthenticatedWebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Uses the authenticated web client to pull all channels for a given project
     * from the api and convert them to POJOs
     * @param projectId the project to get channels for
     * @return a Set of Channels (should have at minimum one entry)
     * @throws IllegalArgumentException when the web client receives a bad parameter
     * @throws IOException When the AuthenticatedWebClient receives and error response code
     */
    public Set<Channel> getChannelsByProjectId(String projectId) throws IllegalArgumentException, IOException {
        HashSet<Channel> channels = new HashSet<Channel>();
        AuthenticatedWebClient.WebResponse response = webClient.get("api/projects/" + projectId + "/channels");
        if (response.isErrorCode()) {
            throw new IOException(String.format("Code %s - %n%s", response.getCode(), response.getContent()));
        }
        JSONObject json = (JSONObject)JSONSerializer.toJSON(response.getContent());
        for (Object obj : json.getJSONArray("Items")) {
            JSONObject jsonObj = (JSONObject)obj;
            String id = jsonObj.getString("Id");
            String name = jsonObj.getString("Name");
            String description = jsonObj.getString("Description");
            boolean isDefault = jsonObj.getBoolean("IsDefault");
            channels.add(new Channel(id, name, description, projectId, isDefault));
        }
        return channels;
    }
    
    /**
     * Uses the authenticated web client to pull a channel by name from a given project
     * from the api and convert them to POJOs
     * @param projectId the project to get channels for
     * @param channelName the channel to return
     * @return the named channel for the given project
     * @throws IllegalArgumentException when the web client receives a bad parameter
     * @throws IOException When the AuthenticatedWebClient receives and error response code
     */
    public Channel getChannelByName(String projectId, String channelName) throws IllegalArgumentException, IOException {
        AuthenticatedWebClient.WebResponse response = webClient.get("api/projects/" + projectId + "/channels");
        if (response.isErrorCode()) {
            throw new IOException(String.format("Code %s - %n%s", response.getCode(), response.getContent()));
        }
        JSONObject json = (JSONObject)JSONSerializer.toJSON(response.getContent());
        for (Object obj : json.getJSONArray("Items")) {
            JSONObject jsonObj = (JSONObject)obj;
            String id = jsonObj.getString("Id");
            String name = jsonObj.getString("Name");
            String description = jsonObj.getString("Description");
            boolean isDefault = jsonObj.getBoolean("IsDefault");
            if (channelName.equals(name))
            {
                return new Channel(id, name, description, projectId, isDefault);
            }
        }
        return null;
    }
}
