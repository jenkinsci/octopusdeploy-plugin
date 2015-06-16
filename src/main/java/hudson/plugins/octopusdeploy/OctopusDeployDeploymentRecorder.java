package hudson.plugins.octopusdeploy;
import com.octopusdeploy.api.*;
import hudson.Launcher;
import hudson.Extension;
import hudson.model.*;
import hudson.tasks.*;
import hudson.util.*;
import java.io.*;
import java.util.Set;
import jenkins.model.Jenkins;
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
     * Whether or not perform will return control immediately, or wait until the Deployment
     * task is completed.
     */
    private final boolean waitForDeployment;
    public boolean getWaitForDeployment() {
        return waitForDeployment;
    }
    
    @DataBoundConstructor
    public OctopusDeployDeploymentRecorder(String project, String releaseVersion, String environment, boolean waitForDeployment) {
        this.project = project.trim();
        this.releaseVersion = releaseVersion.trim();
        this.environment = environment.trim();
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
        log.info("Started Octopus Deploy");
        log.info("======================");
        log.info("Project: " + project);
        log.info("Version: " + releaseVersion);
        log.info("Environment: " + environment);
        log.info("======================");
        ((DescriptorImpl)getDescriptor()).setGlobalConfiguration();
        OctopusApi api = new OctopusApi(((DescriptorImpl)getDescriptor()).octopusHost, ((DescriptorImpl)getDescriptor()).apiKey);
        
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
        try {
            String results = api.executeDeployment(releaseToDeploy.getId(), env.getId());
            if (waitForDeployment && isTaskJson(results)) {
                log.info("Waiting for deployment to complete.");
                String resultState = waitForDeploymentCompletion(JSONSerializer.toJSON(results), api, log);
                if (resultState == null) {
                    log.info("Marking build failed due to failure in waiting for deployment to complete.");
                    success = false;
                }
                    
                if ("Failed".equals(resultState)) {
                    log.info("Marking build failed due to deployment task status.");
                    success = false;
                }
            }
            log.info(results);
        } catch(IOException ex) {
            log.fatal("Failed to deploy: " + ex.getMessage());
            success = false;
        }
        
        return success;
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
        } catch (JSONException ex)
        {
            return false;
        }
    }
    
    /**
     * Returns control when task is complete.
     * @param json
     * @param logger 
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
                OctopusDeployPlugin.DescriptorImpl descriptor = (OctopusDeployPlugin.DescriptorImpl) 
                       Jenkins.getInstance().getDescriptor(OctopusDeployPlugin.class );
                apiKey = descriptor.getApiKey();
                octopusHost = descriptor.getOctopusHost();
                loadedConfig = true;
            }
        }
        
        /**
         * Check that the project field is not empty and is a valid project.
         * @param project The name of the project.
         * @return Ok if not empty, error otherwise.
         */
        public FormValidation doCheckProject(@QueryParameter String project) {
            setGlobalConfiguration(); 
            project = project.trim(); // TODO: Extract this to be shared between plugins
            if (project.isEmpty()) {
                return FormValidation.error("Please provide a project name.");
            }
            OctopusApi api = new OctopusApi(octopusHost, apiKey);
            try {
                com.octopusdeploy.api.Project p = api.getProjectByName(project, true);
                if (p == null)
                {
                    return FormValidation.error("Project not found.");
                }
                if (!project.equals(p.getName()))
                {
                    return FormValidation.warning("Project name case does not match. Did you mean '%s'?", p.getName());
                }
            } catch (IllegalArgumentException ex) {
                return FormValidation.error(ex.getMessage());
            } catch (IOException ex) {
                return FormValidation.error(ex.getMessage());
            }
            return FormValidation.ok();
        }
        
        /**
         * Check that the releaseVersion field is not empty.
         * @param releaseVersion The release version of the package.
         * @return Ok if not empty, error otherwise.
         */
        public FormValidation doCheckReleaseVersion(@QueryParameter String releaseVersion) {
            if ("".equals(releaseVersion)) {
                return FormValidation.error("Please provide a release version.");
            }
            return FormValidation.ok();
        }
        
        /**
         * Check that the environment field is not empty.
         * @param environment The name of the project.
         * @return Ok if not empty, error otherwise.
         */
        public FormValidation doCheckEnvironment(@QueryParameter String environment) {
            setGlobalConfiguration();
            // TODO: Extract this to be shared between plugins
            // TODO: Deduplicate this with project check
            environment = environment.trim(); 
            if (environment.isEmpty()) {
                return FormValidation.error("Please provide an environment name.");
            }
            OctopusApi api = new OctopusApi(octopusHost, apiKey);
            try {
                com.octopusdeploy.api.Environment env = api.getEnvironmentByName(environment, true);
                if (env == null)
                {
                    return FormValidation.error("Environment not found.");
                }
                if (!environment.equals(env.getName()))
                {
                    return FormValidation.warning("Environment name case does not match. Did you mean '%s'?", env.getName());
                }
            } catch (IllegalArgumentException ex) {
                return FormValidation.error(ex.getMessage());
            } catch (IOException ex) {
                return FormValidation.error(ex.getMessage());
            }
            return FormValidation.ok();
        }
    }
}
