package com.octopusdeploy.api;

public class OctopusApi {
    private final AuthenticatedWebClient webClient;
    
    private final ChannelsApi channelsApi;
    public ChannelsApi getChannelsApi() {
        return channelsApi;
    }
    
    private final TenantsApi tenantsApi;
    public TenantsApi getTenantsApi() {
        return tenantsApi;
    }
    
    private final EnvironmentsApi environmentsApi;
    public EnvironmentsApi getEnvironmentsApi() {
        return environmentsApi;
    }
    
    private final ProjectsApi projectsApi;
    public ProjectsApi getProjectsApi() {
        return projectsApi;
    }
    
    private final DeploymentsApi deploymentsApi;
    public DeploymentsApi getDeploymentsApi() {
        return deploymentsApi;
    }
    
    private final ReleasesApi releasesApi;
    public ReleasesApi getReleasesApi() {
        return releasesApi;
    }
    
    private final VariablesApi variablesApi;
    public VariablesApi getVariablesApi() {
        return variablesApi;
    }
    
    private final TasksApi tasksApi;
    public TasksApi getTasksApi() {
        return tasksApi;
    }
    
    public OctopusApi(String octopusHost, String apiKey) {
        webClient = new AuthenticatedWebClient(octopusHost, apiKey);
        channelsApi = new ChannelsApi(webClient);
        tenantsApi = new TenantsApi(webClient);
        environmentsApi = new EnvironmentsApi(webClient);
        projectsApi = new ProjectsApi(webClient);
        deploymentsApi = new DeploymentsApi(webClient);
        releasesApi = new ReleasesApi(webClient);
        variablesApi = new VariablesApi(webClient);
        tasksApi = new TasksApi(webClient);
    }
}
