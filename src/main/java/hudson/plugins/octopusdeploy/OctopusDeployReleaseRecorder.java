package hudson.plugins.octopusdeploy;
import com.octopusdeploy.api.OctopusApi;
import com.octopusdeploy.api.Release;
import hudson.Launcher;
import hudson.Extension;
import hudson.model.*;
import hudson.tasks.*;
import hudson.scm.*;
import hudson.util.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

/**
 * Creates releases
 * @author badriance
 */
public class OctopusDeployReleaseRecorder extends Recorder {
    /**
     * The project name as defined in Octopus.
     */
    private String project;
    public String getProject() {
        return project;
    }

    /**
     * The release version as defined in Octopus.
     */
    private String releaseVersion;
    public String getReleaseVersion() {
        return releaseVersion;
    }

    /**
     * The package version as defined in Octopus.
     */
    private String packageVersion;
    public String getPackageVersion() {
        return packageVersion;
    }

    /**
     * Is there release notes for this release?
     */
    private Boolean releaseNotes;
    public Boolean getReleaseNotes() {
        return releaseNotes;
    }
    
    /**
     * Where are the release notes located?
     */
    private String releaseNotesSource;
    public String getReleaseNotesSource() {
        return releaseNotesSource;
    }

    /**
     * The file that the release notes are in.
     */
    private String releaseNotesFile;
    public String getReleaseNotesFile() {
        return releaseNotesFile;
    }

    /**
     * Should this release be deployed immediately?
     */
    private Boolean deployImmediately;
    public Boolean getDeployImmediately() {
        return deployImmediately;
    }

    /**
     * The environment as defined in Octopus to deploy to.
     */
    private String env;
    public String getEnv() {
        return env;
    }
    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public OctopusDeployReleaseRecorder(String project, String releaseVersion, String packageVersion, boolean releaseNotes, String releaseNotesSource, String releaseNotesFile, boolean deployImmediately, String env) {
        this.project = project;
        this.releaseVersion = releaseVersion;
        this.packageVersion = packageVersion;
        this.releaseNotes = releaseNotes;
        this.releaseNotesSource = releaseNotesSource;
        this.releaseNotesFile = releaseNotesFile;
        this.deployImmediately = deployImmediately;
        this.env = env;
    }
    
    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        boolean success = true;
        Log log = new Log(listener);
        log.info("Started Octopus Release");
        log.info("======================");
        log.info("Project: " + project);
        log.info("Release Version: " + releaseVersion);
        log.info("Package Version: " + packageVersion);
        log.info("Release Notes?: " + releaseNotes);
        log.info("Release Notes Source: " + releaseNotesSource);
        log.info("Release Notes File: " + releaseNotesFile);
        log.info("Deploy this Release Immediately?: " + deployImmediately);
        log.info("Environment: " + env);
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
        if (p == null) {
            log.fatal("Project was not found.");
            success = false;
        }
        
        // Check releaseVersion
        
        // Check packageVersion
        
        if (releaseNotes) {
            if (releaseNotesSource.equals("file")) {
                // Check releaseNotesFile
            } else if (releaseNotesSource.equals("scm")) {
                // Check ...whatever needs to be checked in SCM
            } else {
                // Exit early because misconfigured, should be releaseNotesFromFile ^ releaseNotesFromSCM
                success = false;
            }
        }
        
        com.octopusdeploy.api.Environment environment = null;
        if (deployImmediately) {
            try {
                environment = api.getEnvironmentByName(env);
            } catch (Exception ex) {
                log.fatal(String.format("Retrieving environment name '%s' failed with message '%s'",
                    environment, ex.getMessage()));
                success = false;
            }
        }
        if (environment == null) {
            log.fatal("Environment was not found.");
            success = false;
        }
        
        if (!success) { // Early exit
            return success;
        }
        
        // Do release
        
        return success;
    }
    
    /**
     * Descriptor for {@link OctopusDeployReleaseRecorder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        private String octopusHost;
        private String apiKey;
        private boolean loadedConfig;
        
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "OctopusDeploy Release";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
            save();
            return super.configure(req, formData);
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
         * Check that the project field is not empty.
         * @param project The name of the project.
         * @return Ok if not empty, error otherwise.
         * @throws java.io.IOException
         * @throws javax.servlet.ServletException
         */
        public FormValidation doCheckProject(@QueryParameter String project) throws IOException, ServletException {
            if ("".equals(project))
                return FormValidation.error("Please provide a project name.");
            return FormValidation.ok();
        }
        
        /**
         * Check that the releaseVersion field is not empty.
         * @param releaseVersion The name of the project.
         * @return Ok if not empty, error otherwise.
         * @throws java.io.IOException
         * @throws javax.servlet.ServletException
         */
        public FormValidation doCheckReleaseVersion(@QueryParameter String releaseVersion) throws IOException, ServletException {
            if ("".equals(releaseVersion))
                return FormValidation.error("Please provide a release version.");
            return FormValidation.ok();
        }
        
        /**
         * Check that the releaseNotesFile field is not empty.
         * @param releaseNotesFile The name of the project.
         * @return Ok if not empty, error otherwise.
         * @throws java.io.IOException
         * @throws javax.servlet.ServletException
         */
        public FormValidation doCheckReleaseNotesFile(@QueryParameter String releaseNotesFile) throws IOException, ServletException {
            if ("".equals(releaseNotesFile))
                return FormValidation.error("Please provide a project notes file.");
            return FormValidation.ok();
        }
        
        /**
         * Check that the env field is not empty.
         * @param env The name of the project.
         * @return Ok if not empty, error otherwise.
         * @throws java.io.IOException
         * @throws javax.servlet.ServletException
         */
        public FormValidation doCheckEnv(@QueryParameter String env) throws IOException, ServletException {
            if ("".equals(env))
                return FormValidation.error("Please provide an environment.");
            return FormValidation.ok();
        }
        
        /**
         * Instantiate a new instance of OctopusDeployReleaseRecorder.
         * @param req
         * @param formData
         * @return
         * @throws hudson.model.Descriptor.FormException 
         */
        @Override
        // TODO: What should this return? / Is this even needed?
        public OctopusDeployReleaseRecorder newInstance(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
            return req.bindJSON(clazz, flattenReleaseJSON(formData));
        }
        
        /**
         * Flattens the given release JSON by extracting values.
         * Solves bind issues from the JSON normally passed.
         * All Strings not set in Jenkins will be stored as empty.
         * @param json The release's JSON.
         * @return The flattened release JSON.
         */
        private JSONObject flattenReleaseJSON(JSONObject json) {
            JSONObject flatJson = new JSONObject();
            // Values that are always set
            flatJson.put("project", json.getString("project"));
            flatJson.put("releaseVersion", json.getString("releaseVersion"));
            flatJson.put("stapler-class", json.getString("stapler-class"));
            flatJson.put("$class", json.getString("$class"));
            
            // Values sometimes set (will be empty string if not set)
            flatJson.put("packageVersion", json.getString("packageVersion"));
            
            // releaseNotes : When releasenotes.publisher.value is "on" this is considered true
            JSONObject releaseNotes = json.getJSONObject("releaseNotes");
            if (releaseNotes != null) {
                releaseNotes = releaseNotes.getJSONObject("releaseNotesSource");
            }
            if (releaseNotes.getString("value") != null) { // Release Notes
                flatJson.put("releaseNotes", true);
                if (releaseNotes.getString("value").equals("file")) { // Release Notes from File
                    flatJson.put("releaseNotesFromFile", true);
                    flatJson.put("releaseNotesFile", releaseNotes.getString("releaseNotesFile"));
                    flatJson.put("releaseNotesFromSCM", false);
                } else { // Release Notes from SCM
                    flatJson.put("releaseNotesFromFile", false);
                    flatJson.put("releaseNotesFile", "");
                    flatJson.put("releaseNotesFromSCM", true);
                }
            } else { // No release notes
                flatJson.put("releaseNotes", "false");
            }
            
            JSONObject deployImmediately = json.getJSONObject("deployImmediately");
            if (deployImmediately != null) {
                flatJson.put("deployImmediately", true);
                flatJson.put("env", deployImmediately.getString("env"));
            } else {
                flatJson.put("deployImmediately", false);
            }
            
            return flatJson;
        }
    }
}

