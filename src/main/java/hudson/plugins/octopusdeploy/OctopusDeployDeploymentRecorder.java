package hudson.plugins.octopusdeploy;
import com.octopusdeploy.api.*;
import hudson.Launcher;
import hudson.Extension;
import hudson.model.*;
import hudson.tasks.*;
import hudson.util.*;
import java.io.IOException;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.*;

/**
 * Executes deployments of releases.
 * @author badriance
 */
public class OctopusDeployDeploymentRecorder extends Recorder {
    
    /**
     * The Project name of the project as defined in Octopus.
     */
    private final String project;
    public String getProject() {
        return project;
    }
    
    /**
     * The release version number in octopus.
     */
    private final String releaseVersion;
    public String getReleaseVersion() {
        return releaseVersion;
    }
    
    /**
     * The environment to deploy to in octopus.
     */
    private final String environment;
    public String getEnvironment() {
        return environment;
    }
    
    @DataBoundConstructor
    public OctopusDeployDeploymentRecorder(String project, String releaseVersion, String environment) {
        this.project = project;
        this.releaseVersion = releaseVersion;
        this.environment = environment;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }
    
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        Log log = new Log(listener);
        log.info("Started Octopus Deploy");
        log.info("Project " + project);
        log.info("Version " + releaseVersion);
        log.info("Environment" + environment);
        return true;
    }

    /**
     * Descriptor for {@link OctopusDeployDeploymentRecorder}. Used as a singleton.
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
            return "OctopusDeploy Deployment";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
            save();
            return super.configure(req, formData);
        }
        
        /**
         * Allows plugin user to validate release information by implementing Validate button.
         * @param project
         * @param releaseVersion
         * @param environment 
         * @return A FormValidation object with the validation status and a brief message explaining the status.
         * @throws IOException
         * @throws ServletException 
         */
        public FormValidation doDeployValidation(@QueryParameter("project") final String project,
            @QueryParameter("releaseVersion") final String releaseVersion,
            @QueryParameter("environment") final String environment) {
            // Tests go here, then return one of the following based on results:
            // return FormValidation.ok("This is a Success message");
            // return FormValidation.ok("This is a Warning message");
            // return FormValidation.ok("This is a Error message");
            return FormValidation.ok("I'm a sample success message!");
        }
        
        /**
        * Loads the OctopusDeployPlugin descriptor and pulls configuration from it
        * for API Key, and Host.
        */
        private void setGlobalConfiguration() {
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
            setGlobalConfiguration(); // TODO: Extract this to be shared between plugins
            project = project.trim();
            if ("".equals(project)) {
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
         * @throws java.io.IOException
         * @throws javax.servlet.ServletException
         */
        public FormValidation doCheckEnvironment(@QueryParameter String environment) {
            if ("".equals(environment)) {
                return FormValidation.error("Please provide an environment.");
            }
            return FormValidation.ok();
        }
    }
}
