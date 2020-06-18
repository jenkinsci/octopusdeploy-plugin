package hudson.plugins.octopusdeploy;

import com.google.common.base.Splitter;
import com.octopusdeploy.api.data.*;
import com.octopusdeploy.api.*;
import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import com.octopusdeploy.api.data.Environment;
import com.octopusdeploy.api.data.Project;
import hudson.*;
import hudson.model.*;
import hudson.plugins.octopusdeploy.constants.OctoConstants;
import hudson.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.util.BuildListenerAdapter;
import net.sf.json.*;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.*;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkState;

/**
 * Executes deployments of releases.
 */
public class OctopusDeployDeploymentRecorder extends AbstractOctopusDeployRecorderPostBuildStep implements Serializable {

    /**
     * The release version number in Octopus.
     */
    private final String releaseVersion;
    public String getReleaseVersion() {
        return releaseVersion;
    }

    @DataBoundConstructor
    public OctopusDeployDeploymentRecorder(String serverId, String toolId, String spaceId, String project,
                                           String releaseVersion, String environment) {
        this.serverId = serverId.trim();
        this.toolId = toolId.trim();
        this.spaceId = spaceId.trim();
        this.project = project.trim();
        this.releaseVersion = releaseVersion.trim();
        this.environment = environment.trim();
        this.cancelOnTimeout = false;
        this.waitForDeployment = false;
        this.verboseLogging = false;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) {
        // This method deserves a refactor and cleanup.
        boolean success = true;
        BuildListenerAdapter listenerAdapter = new BuildListenerAdapter(listener);

        Log log = new Log(listenerAdapter);
        if (Result.FAILURE.equals(run.getResult())) {
            log.info("Not deploying due to job being in FAILED state.");
            return;
        }

        EnvVars envVars;
        try {
            envVars = run.getEnvironment(listener);
        } catch (Exception ex) {
            log.fatal(String.format("Failed to retrieve environment variables for this build - '%s'", ex.getMessage()));
            run.setResult(Result.FAILURE);
            return;
        }
        VariableResolver resolver =  new VariableResolver.ByMap<>(envVars);
        EnvironmentVariableValueInjector envInjector = new EnvironmentVariableValueInjector(resolver, envVars);

        // NOTE: hiding the member variables of the same name with their env-injected equivalents
        String project = envInjector.injectEnvironmentVariableValues(this.project);
        String releaseVersion = envInjector.injectEnvironmentVariableValues(this.releaseVersion);
        String environment = envInjector.injectEnvironmentVariableValues(this.environment);
        String tenant = envInjector.injectEnvironmentVariableValues(this.tenant);
        String variables = envInjector.injectEnvironmentVariableValues(this.variables);

        logStartHeader(log);

        checkState(StringUtils.isNotBlank(project), String.format(OctoConstants.Errors.INPUT_CANNOT_BE_BLANK_MESSAGE_FORMAT, "Project name"));
        checkState(StringUtils.isNotBlank(environment), String.format(OctoConstants.Errors.INPUT_CANNOT_BE_BLANK_MESSAGE_FORMAT, "Environment name"));
        checkState(StringUtils.isNotBlank(releaseVersion), String.format(OctoConstants.Errors.INPUT_CANNOT_BE_BLANK_MESSAGE_FORMAT, "Version"));

        Properties properties = new Properties();
        if (variables != null && !variables.isEmpty()) {
            try {
                properties.load(new StringReader(variables));
            } catch (Exception ex) {
                log.fatal(String.format("Unable to load entry variables: '%s'", ex.getMessage()));
                run.setResult(Result.FAILURE);
                return;
            }
        }

        final List<String> commands = new ArrayList<>();
        commands.add(OctoConstants.Commands.DEPLOY_RELEASE);

        final Iterable<String> environmentNameSplit = Splitter.on(',')
                .trimResults()
                .omitEmptyStrings()
                .split(environment);
        for(String env : environmentNameSplit) {
            commands.add("--deployTo");
            commands.add(env);
        }

        commands.add("--version");
        commands.add(releaseVersion);

        if(StringUtils.isNotBlank(tenant)) {
            Iterable<String> tenantsSplit = Splitter.on(',')
                    .trimResults()
                    .omitEmptyStrings()
                    .split(tenant);
            for(String t : tenantsSplit) {
                commands.add("--tenant");
                commands.add(t);
            }
        }

        if(StringUtils.isNotBlank(tenantTag)) {
            Iterable<String> tenantTagsSplit = Splitter.on(',')
                    .trimResults()
                    .omitEmptyStrings()
                    .split(tenantTag);
            for (String tag : tenantTagsSplit) {
                commands.add("--tenanttag");
                commands.add(tag);
            }
        }

        if(waitForDeployment) {
            commands.add("--progress");
        }

        for(String variableName : properties.stringPropertyNames()) {
            String variableValue = properties.getProperty(variableName);
            commands.add("--variable");
            commands.add(String.format("%s:%s", variableName, variableValue));
        }

        commands.addAll(getCommonCommandArguments());

        if(success) {
            try {
                final Boolean[] masks = getMasks(commands, OctoConstants.Commands.Arguments.MaskedArguments);
                Result result = launchOcto(workspace, launcher, commands, masks, envVars, listenerAdapter);
                success = result.equals(Result.SUCCESS);
                if(success) {
                    String serverUrl = getOctopusDeployServer(serverId).getUrl();
                    if (serverUrl.endsWith("/")) {
                        serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
                    }

                    OctopusApi api = getOctopusDeployServer(serverId).getApi().forSpace(spaceId);
                    Project fullProject = api.getProjectsApi().getProjectByName(project, true);
                    Environment fullEnvironment = api.getEnvironmentsApi().getEnvironmentByName(environment, true);

                    String tenantId = null;
                    if (tenant != null && !tenant.isEmpty()) {
                        Tenant fullTenant = api.getTenantsApi().getTenantByName(tenant, true);
                        tenantId = fullTenant.getId();
                    }

                    String urlSuffix = api.getDeploymentsApi().getPortalUrlForDeployment(fullProject.getId(), releaseVersion, fullEnvironment.getId(), tenantId);

                    if (urlSuffix != null && !urlSuffix.isEmpty()) {
                        String portalUrl = serverUrl + urlSuffix;
                        log.info("Deployment executed: \n\t" + portalUrl);
                        run.addAction(new BuildInfoSummary(BuildInfoSummary.OctopusDeployEventType.Deployment, portalUrl));
                    }
                }
            } catch (Exception ex) {
                log.fatal("Failed to deploy: " + ex.getMessage());
                success = false;
            }
        }

        if (!success) {
            run.setResult(Result.FAILURE);
        }
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
        if (tenant != null && !tenant.isEmpty()) {
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
     * @return the task state for the deployment
     */
    private String waitForDeploymentCompletion(JSON json, OctopusApi api, Log logger) {
        final long WAIT_TIME = 5000;
        final double WAIT_RANDOM_SCALER = 100.0;
        JSONObject jsonObj = (JSONObject)json;
        String id = jsonObj.getString("TaskId");
        Task task = null;
        String lastState = "Unknown";
        try {
            task = api.getTasksApi().getTask(id);
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
                task = api.getTasksApi().getTask(id);
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
    @Symbol("octopusDeployRelease")
    public static final class DescriptorImpl extends AbstractOctopusDeployDescriptorImplPost {
        private static final String PROJECT_RELEASE_VALIDATION_MESSAGE = "Project must be set to validate release.";
        private static final String SERVER_ID_VALIDATION_MESSAGE = "Could not validate without a valid Server ID.";

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Octopus Deploy: Deploy Release";
        }

        /**
         * Check that the project field is not empty and is a valid project.
         * @param project The name of the project.
         * @param serverId The id of OctopusDeployServer in the configuration.
         * @param spaceId The id of the space where to load this resource from
         * @return Ok if not empty, error otherwise.
         */
        public FormValidation doCheckProject(@QueryParameter String project, @QueryParameter String serverId, @QueryParameter String spaceId) {
            project = project.trim();

            if (doCheckServerId(serverId).kind != FormValidation.Kind.OK) {
                return FormValidation.warning(SERVER_ID_VALIDATION_MESSAGE);
            }

            OctopusApi api = getApiByServerId(serverId).forSpace(spaceId);
            OctopusValidator validator = new OctopusValidator(api);
            return validator.validateProject(project);
        }

        /**
         * Check that the deployment timeout is valid.
         * @param deploymentTimeout The deployment timeout (TimeSpan).
         * @return Ok if not empty, error otherwise.
         */
        public FormValidation doCheckDeploymentTimeout(@QueryParameter String deploymentTimeout) {
            return OctopusValidator.validateDeploymentTimeout(deploymentTimeout);
        }

        /**
         * Check that the releaseVersion field is not empty.
         * @param releaseVersion The release version of the package.
         * @param project The project name
         * @param serverId The id of OctopusDeployServer in the configuration.
         * @param spaceId The id of the space where to load this resource from
         * @return Ok if not empty, error otherwise.
         */
        public FormValidation doCheckReleaseVersion(@QueryParameter String releaseVersion, @QueryParameter String project, @QueryParameter String serverId, @QueryParameter String spaceId) {
            releaseVersion = releaseVersion.trim();

            if (doCheckServerId(serverId).kind != FormValidation.Kind.OK) {
                return FormValidation.warning(SERVER_ID_VALIDATION_MESSAGE);
            }

            OctopusApi api = getApiByServerId(serverId).forSpace(spaceId);
            if (project == null || project.isEmpty()) {
                return FormValidation.warning(PROJECT_RELEASE_VALIDATION_MESSAGE);
            }
            com.octopusdeploy.api.data.Project p;
            try {
                p = api.getProjectsApi().getProjectByName(project);
                if (p == null) {
                    return FormValidation.warning(PROJECT_RELEASE_VALIDATION_MESSAGE);
                }
            } catch (Exception ex) {
                return FormValidation.warning(PROJECT_RELEASE_VALIDATION_MESSAGE);
            }

            OctopusValidator validator = new OctopusValidator(api);
            return validator.validateRelease(releaseVersion, p, OctopusValidator.ReleaseExistenceRequirement.MustExist);
        }


        /**
         * Check that the environment field is not empty.
         * @param environment The name of the project.
         * @param serverId The id of OctopusDeployServer in the configuration.
         * @param spaceId The id of the space where to load this resource from
         * @return Ok if not empty, error otherwise.
         */
        public FormValidation doCheckEnvironment(@QueryParameter String environment, @QueryParameter String serverId, @QueryParameter String spaceId) {
            environment = environment.trim();

            if (doCheckServerId(serverId).kind != FormValidation.Kind.OK) {
                return FormValidation.warning(SERVER_ID_VALIDATION_MESSAGE);
            }

            OctopusApi api = getApiByServerId(serverId).forSpace(spaceId);
            OctopusValidator validator = new OctopusValidator(api);
            return validator.validateEnvironment(environment);
        }

        /**
         * Data binding that returns all possible environment names to be used in the environment autocomplete.
         * @param serverId The id of OctopusDeployServer in the configuration.
         * @param spaceId The id of the space where to load this resource from
         * @return ComboBoxModel
         */
        public ListBoxModel doFillEnvironmentItems(@QueryParameter String serverId, @QueryParameter String spaceId) {
            ListBoxModel names = new ListBoxModel();

            if (doCheckServerId(serverId).kind != FormValidation.Kind.OK) {
                return names;
            }

            OctopusApi api = getApiByServerId(serverId).forSpace(spaceId);
            try {
                Set<com.octopusdeploy.api.data.Environment> environments = api.getEnvironmentsApi().getAllEnvironments();
                for (com.octopusdeploy.api.data.Environment env : environments) {
                    names.add(env.getName());
                }
            } catch (Exception ex) {
                Logger.getLogger(OctopusDeployDeploymentRecorder.class.getName()).log(Level.SEVERE, null, ex);
            }
            return names;
        }

        /**
         * Data binding that returns all possible project names to be used in the project autocomplete.
         * @param serverId The id of OctopusDeployServer in the configuration.
         * @param spaceId The id of the space where to load this resource from
         * @return ListBoxModel
         */
        public ListBoxModel doFillProjectItems(@QueryParameter String serverId, @QueryParameter String spaceId) {
            ListBoxModel names = new ListBoxModel();

            if (doCheckServerId(serverId).kind != FormValidation.Kind.OK) {
                return names;
            }

            OctopusApi api = getApiByServerId(serverId).forSpace(spaceId);
            try {
                Set<com.octopusdeploy.api.data.Project> projects = api.getProjectsApi().getAllProjects();
                for (com.octopusdeploy.api.data.Project proj : projects) {
                    names.add(proj.getName());
                }
            } catch (Exception ex) {
                Logger.getLogger(OctopusDeployDeploymentRecorder.class.getName()).log(Level.SEVERE, null, ex);
            }
            return names;
        }

        /**
         * Data binding that returns all possible tenant names to be used in the tenant autocomplete.
         * @param serverId The id of OctopusDeployServer in the configuration.
         * @param spaceId The id of the space where to load this resource from
         * @return ComboBoxModel
         */
        public ComboBoxModel doFillTenantItems(@QueryParameter String serverId, @QueryParameter String spaceId) {
            ComboBoxModel names = new ComboBoxModel();

            if (doCheckServerId(serverId).kind != FormValidation.Kind.OK) {
                return names;
            }

            OctopusApi api = getApiByServerId(serverId).forSpace(spaceId);
            try {
                Set<com.octopusdeploy.api.data.Tenant> tenants = api.getTenantsApi().getAllTenants();
                for (com.octopusdeploy.api.data.Tenant ten : tenants) {
                    names.add(ten.getName());
                }
            } catch (Exception ex) {
                Logger.getLogger(OctopusDeployDeploymentRecorder.class.getName()).log(Level.SEVERE, null, ex);
            }
            return names;
        }

        /**
         * Data binding that returns all possible tenant tags to be used in the tenant tag autocomplete.
         * @param serverId The id of OctopusDeployServer in the configuration.
         * @param spaceId The id of the space where to load this resource from
         * @return ComboBoxModel
         */
        public ComboBoxModel doFillTenantTagItems(@QueryParameter String serverId, @QueryParameter String spaceId) {
            ComboBoxModel names = new ComboBoxModel();

            if (doCheckServerId(serverId).kind != FormValidation.Kind.OK) {
                return names;
            }

            OctopusApi api = getApiByServerId(serverId).forSpace(spaceId);
            try {
                Set<TagSet> tagSets = api.getTagSetsApi().getAll();
                for (TagSet tagSet : tagSets) {
                    for (Tag tag : tagSet.getTags()) {
                        names.add(tag.getCanonicalName());
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(OctopusDeployReleaseRecorder.class.getName()).log(Level.SEVERE, null, ex);
            }

            return names;
        }

    }
}
