package com.octopusdeploy.api;

import java.io.IOException;
import java.util.*;
import net.sf.json.*;

public class OctopusApi {

	private final AuthenticatedWebClient webClient;

	public OctopusApi(String octopusHost, String apiKey) {
            webClient = new AuthenticatedWebClient(octopusHost, apiKey);
            // https://github.com/OctopusDeploy/OctopusDeploy-Api/wiki/Authentication
            // set http header: X-Octopus-ApiKey
            // http://docs.oracle.com/javase/7/docs/api/java/net/URLConnection.html
	}

	public void createRelease(String project, String releaseVersion, String releaseNotes, String packageVersion) {
            // https://github.com/OctopusDeploy/OctopusDeploy-Api/wiki/Releases
            // use api to get list of projects, map project name to id
            // repeat this general process to get all the other goodies needed in other methods
	}
	
	public void executeDeployment(String project, String releaseVersion, String environment) {
            // https://github.com/OctopusDeploy/OctopusDeploy-Api/wiki/Deployments
	}
        
        public Set<Project> getAllProjects() throws IllegalArgumentException, IOException {
            HashSet<Project> projects = new HashSet<Project>();
            JSONArray json = (JSONArray)webClient.MakeRequest(AuthenticatedWebClient.GET, "api/projects/all");
            for (Object obj : json) {
                JSONObject jsonObj = (JSONObject)obj;
                String id = jsonObj.getString("Id");
                String name = jsonObj.getString("Name");
                projects.add(new Project(id, name));
            }
            return projects;
        }
        
        public Project getProjectByName(String name)  throws IllegalArgumentException, IOException {
            Set<Project> allProjects = getAllProjects();
            for (Project project : allProjects) {
                if (name.equals(project.getName())) {
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
            JSONArray json = (JSONArray)webClient.MakeRequest(AuthenticatedWebClient.GET, "api/environments/all");
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
         * @param name The name of the Environment to find.
         * @return The Environment with that name.
         * @throws IllegalArgumentException
         * @throws IOException 
         */
        public Environment getEnvironmentByName(String name) throws IllegalArgumentException, IOException {
            Set<Environment> environments = getAllEnvironments();
            for (Environment env : environments) {
                if (name.equals(env.getName())) {
                    return env;
                }
            }
            return null;
        }
}
