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
        
}
