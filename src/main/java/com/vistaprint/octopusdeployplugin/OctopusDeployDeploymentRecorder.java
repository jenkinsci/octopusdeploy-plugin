package com.vistaprint.octopusdeployplugin;
import hudson.Launcher;
import hudson.Extension;
import hudson.model.*;
import hudson.tasks.*;
import hudson.util.*;
import java.io.IOException;
import javax.servlet.ServletException;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

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
     * The release version number in octopus.
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
       return true;
    }

    /**
     * Descriptor for {@link OctopusDeployDeploymentRecorder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
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
            @QueryParameter("environment") final String environment) throws IOException, ServletException {
            // Tests go here, then return one of the following based on results:
            // return FormValidation.ok("This is a Success message");
            // return FormValidation.ok("This is a Warning message");
            // return FormValidation.ok("This is a Error message");
            return FormValidation.ok("I'm a sample success message!");
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
         * @param releaseVersion The release version of the package.
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
         * Check that the environment field is not empty.
         * @param environment The name of the project.
         * @return Ok if not empty, error otherwise.
         * @throws java.io.IOException
         * @throws javax.servlet.ServletException
         */
        public FormValidation doCheckEnvironment(@QueryParameter String environment) throws IOException, ServletException {
            if ("".equals(environment))
                return FormValidation.error("Please provide an environment.");
            return FormValidation.ok();
        }
    }
}
