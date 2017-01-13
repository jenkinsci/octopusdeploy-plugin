package hudson.plugins.octopusdeploy;

import com.octopusdeploy.api.*;
import java.io.*;
import java.util.*;
import jenkins.model.Jenkins;
import hudson.*;
import hudson.model.*;
import hudson.tasks.*;
import hudson.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.*;
import org.kohsuke.stapler.*;

/**
 * Executes deployments of releases.
 */
public class OctopusDeployDeploymentRecorder extends Recorder implements Serializable {

    /**
     * The Project name of the project as defined in Octopus.
     */
    private final String project;
    public String getProject() {
        return project;
    }

    /**
     * The release version number in Octopus.
     */
    private final String releaseVersion;
    public String getReleaseVersion() {
        return releaseVersion;
    }

    /**
     * The environment to deploy to in Octopus.
     */
    private final String environment;
    public String getEnvironment() {
        return environment;
    }

    /**
     * The variables to use for a deploy to in Octopus.
     */
    private final String variables;
    public String getVariables() {
        return variables;
    }

    /**
     * The Tenant to use for a deploy to in Octopus.
     */
    private final String tenant;
    public String getTenant() {
        return tenant;
    }
    
    /**
     * Whether or not perform will return control immediately, or wait until the Deployment
     * task is completed.
     */
    private final boolean waitForDeployment;
    public boolean getWaitForDeployment() {
        return waitForDeployment;
    }

    @DataBoundConstructor
    public OctopusDeployDeploymentRecorder(String project, String releaseVersion, String environment, String tenant, String variables, boolean waitForDeployment) {
        this.project = project.trim();
        this.releaseVersion = releaseVersion.trim();
        this.environment = environment.trim();
        this.tenant = tenant.trim();
        this.variables = variables.trim();
        this.waitForDeployment = waitForDeployment;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        // This method deserves a refactor and cleanup.
        boolean success = true;
        Log log = new Log(listener);
        if (Result.FAILURE.equals(build.getResult())) {
            log.info("Not deploying due to job being in FAILED state.");
            return success;
        }

        logStartHeader(log);
        // todo: getting from descriptor is ugly. refactor?
        getDescriptorImpl().setGlobalConfiguration();
        OctopusApi api = getDescriptorImpl().api;

        VariableResolver resolver = build.getBuildVariableResolver();
        EnvVars envVars;
        try {
            envVars = build.getEnvironment(listener);
        } catch (Exception ex) {
            log.fatal(String.format("Failed to retrieve environment variables for this build - '%s'", ex.getMessage()));
            return false;
        }
        EnvironmentVariableValueInjector envInjector = new EnvironmentVariableValueInjector(resolver, envVars);
        // NOTE: hiding the member variables of the same name with their env-injected equivalents
        String project = envInjector.injectEnvironmentVariableValues(this.project);
        String releaseVersion = envInjector.injectEnvironmentVariableValues(this.releaseVersion);
        String environment = envInjector.injectEnvironmentVariableValues(this.environment);
        String tenant = envInjector.injectEnvironmentVariableValues(this.tenant);
        String variables = envInjector.injectEnvironmentVariableValues(this.variables);

        com.octopusdeploy.api.Project p = null;
        try {
            p = api.getProjectByName(project);
        } catch (Exception ex) {
            log.fatal(String.format("Retrieving project name '%s' failed with message '%s'",
                    project, ex.getMessage()));
            success = false;
        }
        com.octopusdeploy.api.Environment env = null;
        try {
            env = api.getEnvironmentByName(environment);
        } catch (Exception ex) {
            log.fatal(String.format("Retrieving environment name '%s' failed with message '%s'",
                    environment, ex.getMessage()));
            success = false;
        }

        if (p == null) {
            log.fatal("Project was not found.");
            success = false;
        }
        if (env == null) {
            log.fatal("Environment was not found.");
            success = false;
        }
        if (!success) // Early exit
        {
            return success;
        }

        String tenantId = null;
        if (tenant != null && !tenant.isEmpty()) {
            com.octopusdeploy.api.Tenant ten = null;
            try {
                ten = api.getTenantByName(tenant);
                if (ten != null) {
                    tenantId = ten.getId();
                } else {
                    log.fatal(String.format("Retrieving tenant name '%s' failed with message 'not found'", tenant));
                    return false;
                }
            } catch (Exception ex) {
                log.fatal(String.format("Retrieving tenant name '%s' failed with message '%s'",
                        tenant, ex.getMessage()));
                return false;
            }
        }

        Set<com.octopusdeploy.api.Release> releases = null;
        try {
            releases = api.getReleasesForProject(p.getId());
        } catch (Exception ex) {
            log.fatal(String.format("Retrieving releases for project '%s' failed with message '%s'",
                    project, ex.getMessage()));
            success = false;
        }
        if (releases == null) {
            log.fatal("Releases was not found.");
            return false;
        }
        Release releaseToDeploy = null;
        for(Release r : releases) {
            if (releaseVersion.equals(r.getVersion()))
            {
                releaseToDeploy = r;
                break;
            }
        }
        if (releaseToDeploy == null) // early exit
        {
            log.fatal(String.format("Unable to find release version %s for project %s", releaseVersion, project));
            return false;
        }
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(variables));
        } catch (Exception ex) {
            log.fatal(String.format("Unable to load entry variables failed with message '%s'",
                    ex.getMessage()));
            success = false;
        }

        // TODO: Can we tell if we need to call? For now I will always try and get variable and use if I find them
        Set<com.octopusdeploy.api.Variable> variablesForDeploy = null;

        try {
            String releaseId = releaseToDeploy.getId();
            String environmentId = env.getId();
            variablesForDeploy = api.getVariablesByReleaseAndEnvironment(releaseId, environmentId, properties);
        } catch (Exception ex) {
            log.fatal(String.format("Retrieving variables for release '%s' to environment '%s' failed with message '%s'",
                    releaseToDeploy.getId(), env.getName(), ex.getMessage()));
            success = false;
        }
        try {
            String results = api.executeDeployment(releaseToDeploy.getId(), env.getId(), tenantId, variablesForDeploy);
            if (isTaskJson(results)) {
                JSON resultJson = JSONSerializer.toJSON(results);
                String urlSuffix = ((JSONObject)resultJson).getJSONObject("Links").getString("Web");
                String url = getDescriptorImpl().octopusHost;
                if (url.endsWith("/")) {
                    url = url.substring(0, url.length() - 2);
                }
                log.info("Deployment executed: \n\t" + url + urlSuffix);
                build.addAction(new BuildInfoSummary(BuildInfoSummary.OctopusDeployEventType.Deployment, url + urlSuffix));
                if (waitForDeployment) {

                    log.info("Waiting for deployment to complete.");
                    String resultState = waitForDeploymentCompletion(resultJson, api, log);
                    if (resultState == null) {
                        log.info("Marking build failed due to failure in waiting for deployment to complete.");
                        success = false;
                    }

                    if ("Failed".equals(resultState)) {
                        log.info("Marking build failed due to deployment task status.");
                        success = false;
                    }
                }
            }
        } catch (IOException ex) {
            log.fatal("Failed to deploy: " + ex.getMessage());
            success = false;
        }

        return success;
    }

    private DescriptorImpl getDescriptorImpl() {
        return ((DescriptorImpl)getDescriptor());
    }


    /**
     * Write the startup header for the logs to show what our inputs are.
     * @param log The logger
     */
    private void logStartHeader(Log log) {
        log.info("Started Octopus Deploy");
        log.info("======================");
        log.info("Project: " + project);
        log.info("Version: " + releaseVersion);
        log.info("Environment: " + environment);
        if (tenant!=null && !tenant.isEmpty()) {
            log.info("Tenant: " + tenant);
        }
        log.info("======================");
    }

    /**
     * Attempts to parse the string as JSON.
     * returns true on success
     * @param possiblyJson A string that may be JSON
     * @return true or false. True if string is valid JSON.
     */
    private boolean isTaskJson(String possiblyJson) {
        try {
            JSONSerializer.toJSON(possiblyJson);
            return true;
        } catch (JSONException ex) {
            return false;
        }
    }

    /**
     * Returns control when task is complete.
     * @param json json input
     * @param api octopus api
     * @param logger logger
     */
    private String waitForDeploymentCompletion(JSON json, OctopusApi api, Log logger) {
        final long WAIT_TIME = 5000;
        final double WAIT_RANDOM_SCALER = 100.0;
        JSONObject jsonObj = (JSONObject)json;
        String id = jsonObj.getString("TaskId");
        Task task = null;
        String lastState = "Unknown";
        try {
            task = api.getTask(id);
        } catch (IOException ex) {
            logger.error("Error getting task: " + ex.getMessage());
            return null;
        }

        logger.info("Task info:");
        logger.info("\tId: " + task.getId());
        logger.info("\tName: " + task.getName());
        logger.info("\tDesc: " + task.getDescription());
        logger.info("\tState: " + task.getState());
        logger.info("\n\nStarting wait...");
        boolean completed = task.getIsCompleted();
        while (!completed)
        {
            try {
                task = api.getTask(id);
            } catch (IOException ex) {
                logger.error("Error getting task: " + ex.getMessage());
                return null;
            }

            completed = task.getIsCompleted();
            lastState = task.getState();
            logger.info("Task state: " + lastState);
            if (completed) {
                break;
            }
            try {
                Thread.sleep(WAIT_TIME + (long)(Math.random() * WAIT_RANDOM_SCALER));
            } catch (InterruptedException ex) {
                logger.info("Wait interrupted!");
                logger.info(ex.getMessage());
                completed = true; // bail out of wait loop
            }
        }
        logger.info("Wait complete!");
        return lastState;
    }

    /**
     * Descriptor for {@link OctopusDeployDeploymentRecorder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        private String octopusHost;
        private String apiKey;
        private boolean loadedConfig;
        private OctopusApi api;
        private static final String PROJECT_RELEASE_VALIDATION_MESSAGE = "Project must be set to validate release.";

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "OctopusDeploy Deployment";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
            save();
            return true;
        }

        /**
        * Loads the OctopusDeployPlugin descriptor and pulls configuration from it
        * for API Key, and Host.
        */
        private void setGlobalConfiguration() {
            // NOTE  - This method is not being called from the constructor due
            // to a circular dependency issue on startup
            if (!loadedConfig) {
                updateGlobalConfiguration();
            }
        }

        public void updateGlobalConfiguration() {
            OctopusDeployPlugin.DescriptorImpl descriptor = (OctopusDeployPlugin.DescriptorImpl)
                    Jenkins.getInstance().getDescriptor(OctopusDeployPlugin.class);
             apiKey = descriptor.getApiKey();
             octopusHost = descriptor.getOctopusHost();
             api = new OctopusApi(octopusHost, apiKey);
             loadedConfig = true;
        }

        /**
         * Check that the project field is not empty and is a valid project.
         * @param project The name of the project.
         * @return Ok if not empty, error otherwise.
         */
        public FormValidation doCheckProject(@QueryParameter String project) {
            setGlobalConfiguration();
            project = project.trim();
            OctopusValidator validator = new OctopusValidator(api);
            return validator.validateProject(project);
        }

        /**
         * Check that the releaseVersion field is not empty.
         * @param releaseVersion The release version of the package.
         * @param project The project name
         * @return Ok if not empty, error otherwise.
         */
        public FormValidation doCheckReleaseVersion(@QueryParameter String releaseVersion, @QueryParameter String project) {
            setGlobalConfiguration();
            releaseVersion = releaseVersion.trim();
            if (project == null || project.isEmpty()) {
                return FormValidation.warning(PROJECT_RELEASE_VALIDATION_MESSAGE);
            }
            com.octopusdeploy.api.Project p;
            try {
                p = api.getProjectByName(project);
                if (p == null) {
                    return FormValidation.warning(PROJECT_RELEASE_VALIDATION_MESSAGE);
                }
            } catch (Exception ex) {
                return FormValidation.warning(PROJECT_RELEASE_VALIDATION_MESSAGE);
            }

            OctopusValidator validator = new OctopusValidator(api);
            return validator.validateRelease(releaseVersion, p.getId(), OctopusValidator.ReleaseExistenceRequirement.MustExist);
        }


        /**
         * Check that the environment field is not empty.
         * @param environment The name of the project.
         * @return Ok if not empty, error otherwise.
         */
        public FormValidation doCheckEnvironment(@QueryParameter String environment) {
            setGlobalConfiguration();
            environment = environment.trim();
            OctopusValidator validator = new OctopusValidator(api);
            return validator.validateEnvironment(environment);
        }

        /**
         * Data binding that returns all possible environment names to be used in the environment autocomplete.
         * @return ComboBoxModel
         */
        public ComboBoxModel doFillEnvironmentItems() {
            setGlobalConfiguration();
            ComboBoxModel names = new ComboBoxModel();

            try {
                Set<com.octopusdeploy.api.Environment> environments = api.getAllEnvironments();
                for (com.octopusdeploy.api.Environment env : environments) {
                    names.add(env.getName());
                }
            } catch (Exception ex) {
                Logger.getLogger(OctopusDeployDeploymentRecorder.class.getName()).log(Level.SEVERE, null, ex);
            }
            return names;
        }

        /**
         * Data binding that returns all possible project names to be used in the project autocomplete.
         * @return ComboBoxModel
         */
        public ComboBoxModel doFillProjectItems() {
            setGlobalConfiguration();
            ComboBoxModel names = new ComboBoxModel();
            try {
                Set<com.octopusdeploy.api.Project> projects = api.getAllProjects();
                for (com.octopusdeploy.api.Project proj : projects) {
                    names.add(proj.getName());
                }
            } catch (Exception ex) {
                Logger.getLogger(OctopusDeployDeploymentRecorder.class.getName()).log(Level.SEVERE, null, ex);
            }
            return names;
        }

        /**
         * Data binding that returns all possible tenant names to be used in the tenant autocomplete.
         * @return ComboBoxModel
         */
        public ComboBoxModel doFillTenantItems() {
            setGlobalConfiguration();
            ComboBoxModel names = new ComboBoxModel();
            try {
                Set<com.octopusdeploy.api.Tenant> tenants = api.getAllTenants();
                for (com.octopusdeploy.api.Tenant ten : tenants) {
                    names.add(ten.getName());
                }
            } catch (Exception ex) {
                Logger.getLogger(OctopusDeployDeploymentRecorder.class.getName()).log(Level.SEVERE, null, ex);
            }
            return names;
        }
    }
}
