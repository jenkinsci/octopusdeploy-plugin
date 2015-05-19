package com.vistaprint.octopusapi;

public class OctopusApi {

	private final String octopusHost;
	private final String apiKey;

	public OctopusApi(String octopusHost, String apiKey) {
            this.octopusHost = octopusHost;
            this.apiKey = apiKey;
            // https://github.com/OctopusDeploy/OctopusDeploy-Api/wiki/Authentication
            // set http header: X-Octopus-ApiKey
            // http://docs.oracle.com/javase/7/docs/api/java/net/URLConnection.html
	}

	public void CreateRelease(String project, String releaseVersion, String releaseNotes, String packageVersion) {
            // https://github.com/OctopusDeploy/OctopusDeploy-Api/wiki/Releases
            // use api to get list of projects, map project name to id
            // repeat this general process to get all the other goodies needed in other methods
	}
	
	public void ExecuteDeployment(String project, String releaseVersion, String environment) {
            // https://github.com/OctopusDeploy/OctopusDeploy-Api/wiki/Deployments
	}
        
}
