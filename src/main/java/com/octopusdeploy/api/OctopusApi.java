package com.octopusdeploy.api;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import net.sf.json.*;

public class OctopusApi {

    private final AuthenticatedWebClient webClient;

    public OctopusApi(String octopusHost, String apiKey) {
        webClient = new AuthenticatedWebClient(octopusHost, apiKey);
    }

    public void createRelease(String project, String releaseVersion, String releaseNotes, String packageVersion) {
        // https://github.com/OctopusDeploy/OctopusDeploy-Api/wiki/Releases
        // use api to get list of projects, map project name to id
        // repeat this general process to get all the other goodies needed in other methods
    }

    public String executeDeployment(String releaseId, String environmentId) throws IOException {
        String json = String.format("{EnvironmentId:\"%s\",ReleaseId:\"%s\"}", environmentId, releaseId);
        byte[] data = json.getBytes(Charset.forName("UTF-8"));
        AuthenticatedWebClient.WebResponse response = webClient.post("api/deployments", data);
        if (response.getCode() >= 400) {
            throw new IOException(String.format("Code %s - %n%s", response.getCode(), response.getContent()));
        }
        return response.getContent();
    }

    /**
     * Uses the authenticated web client to pull all projects from the api and
     * convert them to POJOs
     * @return a Set of Projects (may be empty)
     * @throws IllegalArgumentException
     * @throws IOException 
     */
    public Set<Project> getAllProjects() throws IllegalArgumentException, IOException {
        HashSet<Project> projects = new HashSet<Project>();
        AuthenticatedWebClient.WebResponse response = webClient.get("api/projects/all");
        if (response.getCode() >= 400) {
            throw new IOException(String.format("Code %s - %n%s", response.getCode(), response.getContent()));
        }
        JSONArray json = (JSONArray)JSONSerializer.toJSON(response.getContent());
        for (Object obj : json) {
            JSONObject jsonObj = (JSONObject)obj;
            String id = jsonObj.getString("Id");
            String name = jsonObj.getString("Name");
            projects.add(new Project(id, name));
        }
        return projects;
    }

    /**
     * Loads in the full list of projects from the API, then selects one project by name.
     * Only selects the project if the name is an exact match (including case)
     * @param name name of the project to select
     * @return the named project or null if no such project exists
     * @throws IllegalArgumentException
     * @throws IOException 
     */
    public Project getProjectByName(String name)  throws IllegalArgumentException, IOException {
        return getProjectByName(name, false);
    }
    /**
     * Loads in the full list of projects from the API, then selects one project by name.
     * @param name name of the project to select
     * @param ignoreCase when true uses equalsIgnoreCase in the name check
     * @return the named project or null if no such project exists
     * @throws IllegalArgumentException
     * @throws IOException 
     */
    public Project getProjectByName(String name, boolean ignoreCase)  throws IllegalArgumentException, IOException {
        Set<Project> allProjects = getAllProjects();
        for (Project project : allProjects) {
            if ((ignoreCase && name.equalsIgnoreCase(project.getName())) ||
               (!ignoreCase && name.equals(project.getName()))) {
                return project;
            }
        }
        return null;
    }

    /**
     * Get all environments from the Octopus server as Environment objects.
     * @return A set of all environments on the Octopus server.
     * @throws IllegalArgumentException
     * @throws IOException 
     */
    public Set<Environment> getAllEnvironments() throws IllegalArgumentException, IOException {
        HashSet<Environment> environments = new HashSet<Environment>();
        AuthenticatedWebClient.WebResponse response =webClient.get("api/environments/all");
        if (response.getCode() >= 400) {
            throw new IOException(String.format("Code %s - %n%s", response.getCode(), response.getContent()));
        }
        JSONArray json = (JSONArray)JSONSerializer.toJSON(response.getContent());
        for (Object obj : json) {
            JSONObject jsonObj = (JSONObject)obj;
            String id = jsonObj.getString("Id");
            String name = jsonObj.getString("Name");
            String description = jsonObj.getString("Description");
            environments.add(new Environment(id, name, description));
        }
        return environments;
    }

    /**
     * Get the Environment with the given name if it exists, return null otherwise.
     * Only selects the environment if the name is an exact match (including case)
     * @param name The name of the Environment to find.
     * @return The Environment with that name.
     * @throws IllegalArgumentException
     * @throws IOException 
     */
    public Environment getEnvironmentByName(String name) throws IllegalArgumentException, IOException {
        return getEnvironmentByName(name, false);
    }

    /**
     * Get the Environment with the given name if it exists, return null otherwise.
     * @param name The name of the Environment to find.
     * @param ignoreCase when true uses equalsIgnoreCase in the name check
     * @return The Environment with that name.
     * @throws IllegalArgumentException
     * @throws IOException 
     */
    public Environment getEnvironmentByName(String name, boolean ignoreCase) throws IllegalArgumentException, IOException {
        Set<Environment> environments = getAllEnvironments();
        for (Environment env : environments) {
            if ((ignoreCase && name.equalsIgnoreCase(env.getName())) ||
               (!ignoreCase && name.equals(env.getName()))) { 
                return env;
            }
        }
        return null;
    }

    /**
     * Get all releases for a given project from the Octopus server;
     * @param projectId
     * @return A set of all releases for a given project
     * @throws IllegalArgumentException
     * @throws IOException 
     */
    public Set<Release> getReleasesForProject(String projectId) throws IllegalArgumentException, IOException {
        HashSet<Release> releases = new HashSet<Release>();
        AuthenticatedWebClient.WebResponse response = webClient.get("api/projects/" + projectId + "/releases");
        if (response.getCode() >= 400) {
            throw new IOException(String.format("Code %s - %n%s", response.getCode(), response.getContent()));
        }
        JSONObject json = (JSONObject)JSONSerializer.toJSON(response.getContent());
        for (Object obj : json.getJSONArray("Items")) {
            JSONObject jsonObj = (JSONObject)obj;
            String id = jsonObj.getString("Id");
            String version = jsonObj.getString("Version");
            String ReleaseNotes = jsonObj.getString("ReleaseNotes");
            releases.add(new Release(id, projectId, ReleaseNotes, version));
        }
        return releases;
    }
}
