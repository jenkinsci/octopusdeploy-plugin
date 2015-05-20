package com.vistaprint.octopusdeployplugin;
import hudson.Launcher;
import hudson.Extension;
import hudson.model.*;
import hudson.tasks.*;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
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
    }
}
