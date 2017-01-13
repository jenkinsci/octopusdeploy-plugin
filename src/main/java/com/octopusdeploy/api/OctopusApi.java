package com.octopusdeploy.api;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import net.sf.json.*;
import org.apache.commons.lang.StringUtils;

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
     * @throws java.io.IOException When the AuthenticatedWebClient receives and error response code
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
     * @throws java.io.IOException When the AuthenticatedWebClient receives and error response code
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
     * @throws java.io.IOException When the AuthenticatedWebClient receives and error response code
     */
    public String createRelease(String project, String releaseVersion, String releaseNotes, Set<SelectedPackage> selectedPackages) throws IOException {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append(String.format("{ProjectId:\"%s\",Version:\"%s\"", project, releaseVersion));
        if (releaseNotes != null && !releaseNotes.isEmpty()) {
            jsonBuilder.append(String.format(",ReleaseNotes:\"%s\"", releaseNotes));
        }
        if (selectedPackages != null && !selectedPackages.isEmpty()) {
            jsonBuilder.append(",SelectedPackages:[");
            Set<String> selectedPackageStrings = new HashSet<String>();
            for (SelectedPackage selectedPackage : selectedPackages) {
                selectedPackageStrings.add(String.format("{StepName:\"%s\",Version:\"%s\"}", selectedPackage.getStepName(), selectedPackage.getVersion()));
            }
            jsonBuilder.append(StringUtils.join(selectedPackageStrings, ","));
            jsonBuilder.append("]");
        }
        jsonBuilder.append("}");
        String json = jsonBuilder.toString();
        byte[] data = json.getBytes(Charset.forName(UTF8));
        AuthenticatedWebClient.WebResponse response = webClient.post("api/releases", data);
        if (response.isErrorCode()) {
            String errorMsg = ErrorParser.getErrorsFromResponse(response.getContent());
            throw new IOException(String.format("Code %s - %n%s", response.getCode(), errorMsg));
        }
        return response.getContent();
    }

    /**
     * Deploys a given release to provided environment.
     * @param releaseId Release Id from Octopus to deploy.
     * @param environmentId Environment Id from Octopus to deploy to.
     * @param tenantID Tenant Id from Octopus to deploy to.
     * @return the content of the web response.
     * @throws IOException When the AuthenticatedWebClient receives and error response code
     */
    public String executeDeployment(String releaseId, String environmentId, String tenantId) throws IOException {
          return executeDeployment( releaseId,  environmentId, tenantId, null);
    }

    /**
     * Deploys a given release to provided environment.
     * @param releaseId Release Id from Octopus to deploy.
     * @param environmentId Environment Id from Octopus to deploy to.
     * @param tenantID Tenant Id from Octopus to deploy to.
     * @param variables Variables used during deployment.
     * @return the content of the web response.
     * @throws IOException When the AuthenticatedWebClient receives and error response code
     */
    public String executeDeployment(String releaseId, String environmentId, String tenantId, Set<Variable> variables) throws IOException {

        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append(String.format("{EnvironmentId:\"%s\",ReleaseId:\"%s\"", environmentId, releaseId));

        if (tenantId != null && !tenantId.isEmpty()) {
            jsonBuilder.append(String.format(",TenantId:\"%s\"", tenantId));
        }
        if (variables != null && !variables.isEmpty()) {
            jsonBuilder.append(",FormValues:{");
            Set<String> variablesStrings = new HashSet<String>();
            for (Variable v : variables) {
                variablesStrings.add(String.format("\"%s\":\"%s\"", v.getId(), v.getValue()));
            }
            jsonBuilder.append(StringUtils.join(variablesStrings, ","));
            jsonBuilder.append("}");
        }
        jsonBuilder.append("}");
        String json = jsonBuilder.toString();

        byte[] data = json.getBytes(Charset.forName(UTF8));
        AuthenticatedWebClient.WebResponse response = webClient.post("api/deployments", data);
        if (response.isErrorCode()) {
            String errorMsg = ErrorParser.getErrorsFromResponse(response.getContent());
            throw new IOException(String.format("Code %s - %n%s", response.getCode(), errorMsg));
        }
        return response.getContent();
    }

    /**
     * Uses the authenticated web client to pull all projects from the api and
     * convert them to POJOs
     * @return a Set of Projects (may be empty)
     * @throws IllegalArgumentException when the web client receives a bad parameter
     * @throws IOException When the AuthenticatedWebClient receives and error response code
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
     * @throws IllegalArgumentException when the web client receives a bad parameter
     * @throws IOException When the AuthenticatedWebClient receives and error response code
     */
    public Project getProjectByName(String name)  throws IllegalArgumentException, IOException {
        return getProjectByName(name, false);
    }

    /**
     * Loads in the full list of projects from the API, then selects one project by name.
     * @param name name of the project to select
     * @param ignoreCase when true uses equalsIgnoreCase in the name check
     * @return the named project or null if no such project exists
     * @throws IllegalArgumentException when the web client receives a bad parameter
     * @throws IOException When the AuthenticatedWebClient receives and error response code
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
     * @throws IllegalArgumentException when the web client receives a bad parameter
     * @throws IOException When the AuthenticatedWebClient receives and error response code
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
     * @throws IllegalArgumentException when the web client receives a bad parameter
     * @throws IOException When the AuthenticatedWebClient receives and error response code
     */
    public Environment getEnvironmentByName(String name) throws IllegalArgumentException, IOException {
        return getEnvironmentByName(name, false);
    }

    /**
     * Get the Environment with the given name if it exists, return null otherwise.
     * @param name The name of the Environment to find.
     * @param ignoreCase when true uses equalsIgnoreCase in the name check
     * @return The Environment with that name.
     * @throws IllegalArgumentException when the web client receives a bad parameter
     * @throws IOException When the AuthenticatedWebClient receives and error response code
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
     * Uses the authenticated web client to pull all tenants from the api and
     * convert them to POJOs
     * @return a Set of Projects (may be empty)
     * @throws IllegalArgumentException when the web client receives a bad parameter
     * @throws IOException When the AuthenticatedWebClient receives and error response code
     */
    public Set<Tenant> getAllTenants() throws IllegalArgumentException, IOException {
        HashSet<Tenant> tenants = new HashSet<Tenant>();
        AuthenticatedWebClient.WebResponse response = webClient.get("api/tenants/all");
        if (response.isErrorCode()) {
            throw new IOException(String.format("Code %s - %n%s", response.getCode(), response.getContent()));
        }
        JSONArray json = (JSONArray)JSONSerializer.toJSON(response.getContent());
        for (Object obj : json) {
            JSONObject jsonObj = (JSONObject)obj;
            String id = jsonObj.getString("Id");
            String name = jsonObj.getString("Name");
            tenants.add(new Tenant(id, name));
        }
        return tenants;
    }

    /**
     * Get the Tenant with the given name if it exists, return null otherwise.
     * Only selects the tenant if the name is an exact match (including case)
     * @param name The name of the Tenant to find.
     * @return The Tenant with that name.
     * @throws IllegalArgumentException when the web client receives a bad parameter
     * @throws IOException When the AuthenticatedWebClient receives and error response code
     */
    public Tenant getTenantByName(String name) throws IllegalArgumentException, IOException {
        return getTenantByName(name, false);
    }

    /**
     * Get the Tenant with the given name if it exists, return null otherwise.
     * @param name The name of the Tenant to find.
     * @param ignoreCase when true uses equalsIgnoreCase in the name check
     * @return The Environment with that name.
     * @throws IllegalArgumentException when the web client receives a bad parameter
     * @throws IOException When the AuthenticatedWebClient receives and error response code
     */
    public Tenant getTenantByName(String name, boolean ignoreCase) throws IllegalArgumentException, IOException {
        Set<Tenant> tenants = getAllTenants();
        for (Tenant tenant : tenants) {
            if ((ignoreCase && name.equalsIgnoreCase(tenant.getName())) ||
               (!ignoreCase && name.equals(tenant.getName()))) {
                return tenant;
            }
        }
        return null;
    }

    /**
     * Get the variables for a combination of release and environment, return null otherwise.
     * @param releaseId The id of the Release.
     * @param environmentId The id of the Environment.
     * @return A set of all variables for a given Release and Environment combination.
     * @throws IllegalArgumentException when the web client receives a bad parameter
     * @throws IOException When the AuthenticatedWebClient receives and error response code
     */
    public Set<Variable> getVariablesByReleaseAndEnvironment(String releaseId, String environmentId, Properties entryProperties) throws IllegalArgumentException, IOException {
        Set<Variable> variables = new HashSet<Variable>();

        AuthenticatedWebClient.WebResponse response = webClient.get("api/releases/" + releaseId + "/deployments/preview/" + environmentId);
        if (response.isErrorCode()) {
            throw new IOException(String.format("Code %s - %n%s", response.getCode(), response.getContent()));
        }
        JSONObject json = (JSONObject)JSONSerializer.toJSON(response.getContent());
        JSONObject form = json.getJSONObject("Form");
        if (form != null){
            JSONObject formValues = form.getJSONObject("Values");
            for (Object obj : form.getJSONArray("Elements")) {
                JSONObject jsonObj = (JSONObject) obj;
                String id = jsonObj.getString("Name");
                String name = jsonObj.getJSONObject("Control").getString("Name");
                String value = formValues.getString(id);

                String entryValue = entryProperties.getProperty(name);
                if (StringUtils.isNotEmpty(entryValue)) {
                    value = entryValue;
                }
                String description = jsonObj.getJSONObject("Control").getString("Description");
                variables.add(new Variable(id, name, value, description));
            }
        }

        return variables;
    }

    /**
     * Get all releases for a given project from the Octopus server;
     * @param projectId the id of the project to get the releases for
     * @return A set of all releases for a given project
     * @throws IllegalArgumentException when the web client receives a bad parameter
     * @throws IOException When the AuthenticatedWebClient receives and error response code
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
     * @param projectId the id of the project to get the process for.
     * @return DeploymentProcess a representation of the process
     * @throws IllegalArgumentException when the web client receives a bad parameter
     * @throws IOException When the AuthenticatedWebClient receives and error response code
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
     * Return a representation of a deployment process for a given project.
     * @param projectId project id
     * @return DeploymentProcessTemplate deployment process template
     * @throws IllegalArgumentException when the web client receives a bad parameter
     * @throws IOException When the AuthenticatedWebClient receives and error response code
     */
    public DeploymentProcessTemplate getDeploymentProcessTemplateForProject(String projectId) throws IllegalArgumentException, IOException {
        AuthenticatedWebClient.WebResponse response = webClient.get("api/deploymentprocesses/deploymentprocess-" + projectId + "/template");
        if (response.isErrorCode()) {
            throw new IOException(String.format("Code %s - %n%s", response.getCode(), response.getContent()));
        }

        JSONObject json = (JSONObject)JSONSerializer.toJSON(response.getContent());
        Set<SelectedPackage> packages = new HashSet<SelectedPackage>();
        String deploymentId = json.getString("DeploymentProcessId");
        JSONArray pkgsJson = json.getJSONArray("Packages");
        for (Object pkgObj : pkgsJson) {
            JSONObject pkgJsonObj = (JSONObject) pkgObj;
            String name = pkgJsonObj.getString("StepName");
            String version = pkgJsonObj.getString("VersionSelectedLastRelease");
            packages.add(new SelectedPackage(name, version));
        }

        DeploymentProcessTemplate template = new DeploymentProcessTemplate(deploymentId, projectId, packages);
        return template;

    }

    /**
     * Retrieves a task by its id.
     * @param taskId task id
     * @return a Task object
     * @throws IllegalArgumentException  when the web client receives a bad parameter
     * @throws IOException  When the AuthenticatedWebClient receives and error response code
     */
    public Task getTask(String taskId) throws IllegalArgumentException, IOException {
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
