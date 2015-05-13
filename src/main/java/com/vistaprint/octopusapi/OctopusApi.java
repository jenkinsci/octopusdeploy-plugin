package com.vistaprint.octopusapi;

public class OctopusApi {

	private final String octopusHost;
	private final String apiKey;

	public OctopusApi(String octopusHost, String apiKey) {
		this.octopusHost = octopusHost;
		this.apiKey = apiKey;
	}

	public void CreateRelease(String project, String releaseVersion, String releaseNotes, String packageVersion) {
		
	}
	
	public void ExecuteDeployment(String project, String releaseVersion, String environment) {
		
	}
}
