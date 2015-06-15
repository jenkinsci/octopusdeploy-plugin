package com.octopusdeploy.api;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import net.sf.json.*;

public class OctopusApi {
    private final static String UTF8 = "UTF-8";
    private final AuthenticatedWebClient webClient;

    public OctopusApi(String octopusHost, String apiKey) {
        webClient = new AuthenticatedWebClient(octopusHost, apiKey);
    }

    /**
     * Creates a release in octopus deploy.
     * @param project The project id
     * @param releaseVersion The version number for this release.
     * @return content from the API post
     * @throws java.io.IOException
     */
    public String createRelease(String project, String releaseVersion) throws IOException {
        return createRelease(project, releaseVersion, null);
    }
    
    /**
     * Creates a release in octopus deploy.
     * @param project The project id.
     * @param releaseVersion The version number for this release.
     * @param releaseNotes Release notes to be associated with this release.
     * @return content from the API post
     * @throws java.io.IOException
     */
    public String createRelease(String project, String releaseVersion, String releaseNotes) throws IOException {
        return createRelease(project, releaseVersion, releaseNotes, null);
    }
    
    /**
     * Creates a release in octopus deploy.
     * @param project The project id
     * @param releaseVersion The version number for this release.
     * @param releaseNotes Release notes to be associated with this release.
     * @param selectedPackages Packages to be deployed with this release.
     * @return content from the API post
     * @throws java.io.IOException
     */
    public String createRelease(String project, String releaseVersion, String releaseNotes, Set<SelectedPackage> selectedPackages) throws IOException {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append(String.format("{ProjectId:\"%s\",Version:\"%s\"", project, releaseVersion));
        if (releaseNotes != null) {
            jsonBuilder.append(String.format(",ReleaseNotes:\"%s\"", releaseNotes));
        }
        if (selectedPackages != null && !selectedPackages.isEmpty()) {
            jsonBuilder.append(",SelectedPackages:[");
            Set<String> selectedPackageStrings = new HashSet<String>();
            for (SelectedPackage selectedPackage : selectedPackages) {
                selectedPackageStrings.add(String.format("StepName:\"%s\",Version:\"%s\"", selectedPackage.getStepName(), selectedPackage.getVersion()));
            }
            jsonBuilder.append(String.join(",", selectedPackageStrings));
            jsonBuilder.append("]");
        }
        jsonBuilder.append("}");
        String json = jsonBuilder.toString();
        byte[] data = json.getBytes(Charset.forName(UTF8));
        AuthenticatedWebClient.WebResponse response = webClient.post("api/releases", data);
        if (response.isErrorCode()) {
            throw new IOException(String.format("Code %s - %n%s", response.getCode(), response.getContent()));
        }
        return response.getContent();
    }

    /**
     * Deploys a given release to provided environment.
     * @param releaseId Release Id from Octopus to deploy.
     * @param environmentId Environment Id from Octopus to deploy to.
     * @return the content of the web response.
     * @throws IOException 
     */
    public String executeDeployment(String releaseId, String environmentId) throws IOException {
        String json = String.format("{EnvironmentId:\"%s\",ReleaseId:\"%s\"}", environmentId, releaseId);
        byte[] data = json.getBytes(Charset.forName(UTF8));
        AuthenticatedWebClient.WebResponse response = webClient.post("api/deployments", data);
        if (response.isErrorCode()) {
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
        if (response.isErrorCode()) {
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
        if (response.isErrorCode()) {
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
        if (response.isErrorCode()) {
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
    
    /**
     * Return a representation of a deployment process for a given project.
     * @param projectId
     * @return DeploymentProcess
     * @throws IllegalArgumentException
     * @throws IOException 
     */
    public DeploymentProcess getDeploymentProcessForProject(String projectId) throws IllegalArgumentException, IOException {
        // TODO: refactor/method extract/clean up
        AuthenticatedWebClient.WebResponse response = webClient.get("api/deploymentprocesses/deploymentprocess-" + projectId);
        if (response.isErrorCode()) {
            throw new IOException(String.format("Code %s - %n%s", response.getCode(), response.getContent()));
        }
        JSONObject json = (JSONObject)JSONSerializer.toJSON(response.getContent());
        JSONArray stepsJson = json.getJSONArray("Steps");
        HashSet<DeploymentProcessStep> deploymentProcessSteps = new HashSet<DeploymentProcessStep>();
        for (Object stepObj : stepsJson) {
            JSONObject jsonStepObj = (JSONObject)stepObj;
            HashSet<DeploymentProcessStepAction> deploymentProcessStepActions = new HashSet<DeploymentProcessStepAction>();
            
            JSONArray actionsJson = jsonStepObj.getJSONArray("Actions");
            for (Object actionObj : actionsJson) {
                JSONObject jsonActionObj = (JSONObject)actionObj;
                JSONObject propertiesJson = jsonActionObj.getJSONObject("Properties");
                HashMap<String, String> properties = new HashMap<String, String>();
                for (Object key : propertiesJson.keySet()) {
                    String keyString = key.toString();
                    properties.put(keyString, propertiesJson.getString(keyString));
                }
                String dpsaId = jsonActionObj.getString("Id");
                String dpsaName = jsonActionObj.getString("Name");
                String dpsaType = jsonActionObj.getString("ActionType");
                deploymentProcessStepActions.add(new DeploymentProcessStepAction(dpsaId, dpsaName, dpsaType, properties));
            }
            String dpsId = jsonStepObj.getString("Id");
            String dpsName = jsonStepObj.getString("Name");
            deploymentProcessSteps.add(new DeploymentProcessStep(dpsId, dpsName, deploymentProcessStepActions));
        }
        String dpId = json.getString("Id");
        String dpProject = json.getString("ProjectId");
        return new DeploymentProcess(dpId, dpProject, deploymentProcessSteps);
    }
    
    /**
     * Retrieves a task by its id.
     * @param taskId
     * @return a Task object
     * @throws IOException 
     */
    public Task getTask(String taskId) throws IOException {
        AuthenticatedWebClient.WebResponse response = webClient.get("api/tasks/" + taskId);
        if (response.isErrorCode()) {
            throw new IOException(String.format("Code %s - %n%s", response.getCode(), response.getContent()));
        }
        JSONObject json = (JSONObject)JSONSerializer.toJSON(response.getContent());
        String id = json.getString("Id");
        String name = json.getString("Name");
        String description = json.getString("Description");
        String state = json.getString("State");
        boolean isCompleted = json.getBoolean("IsCompleted");
        return new Task(id, name, description, state, isCompleted);
    }
}
