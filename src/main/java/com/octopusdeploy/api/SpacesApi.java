package com.octopusdeploy.api;

import com.octopusdeploy.api.data.Space;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import java.io.IOException;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

public class SpacesApi {
    private final AuthenticatedWebClient webClient;

    public SpacesApi(AuthenticatedWebClient webClient) {
        this.webClient = webClient;
    }

    public boolean getSupportsSpaces() throws IllegalArgumentException, IOException {
        AuthenticatedWebClient.WebResponse response = webClient.getRoot();
        if(response.isErrorCode()) {
            throw new IOException(String.format("Code %s - %n%s", response.getCode(), response.getContent()));
        }

        JSONObject json = (JSONObject) JSONSerializer.toJSON(response.getContent());
        return ((JSONObject)json.get("Links")).has("Spaces");
    }

    public Set<Space> getAllSpaces() throws IllegalArgumentException, IOException {
        TreeSet<Space> spaces = new TreeSet<>(Comparator.comparing(Space::getName).thenComparing(Space::getId));
        AuthenticatedWebClient.WebResponse response = webClient.get("spaces/all");
        if(response.isErrorCode()) {
            throw new IOException(String.format("Code %s - %s%n", response.getCode(), response.getContent()));
        }
        JSONArray json = (JSONArray)JSONSerializer.toJSON(response.getContent());
        for(Object obj : json) {
            JSONObject jsonObj = (JSONObject)obj;
            String id = jsonObj.getString("Id");
            String name = jsonObj.getString("Name");
            spaces.add(new Space(id, name));
        }
        return spaces;
    }
}
