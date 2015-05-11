package com.vistaprint.octopusdeployplugin;
import hudson.Launcher;
import hudson.Extension;
import hudson.model.*;
import hudson.tasks.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * <p>
 * Create a release!
 * </p>
 */
public class OctopusDeployReleaseRecorder extends Recorder {

    private final boolean isActive;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public OctopusDeployReleaseRecorder(boolean isActive) {
        this.isActive = isActive;
    }
    
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }      

    public boolean getIsActive() {
        return isActive;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        
        try {
            if(!isActive) {
                return true;
            }
            
            
            Result result = build.getResult();
            Job  job = build.getParent();
            List<Cause> causes = build.getCauses();
            
            return true;
        } catch (Exception ex) {
             listener.getLogger().println("Error Occured: " + ex.getMessage());
        }
        
         return true;
    }

    /**
     * Descriptor for {@link OctopusDeployReleaseRecorder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        public String getDisplayName() {
            return "OctopusDeploy Release";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
            save();
            return super.configure(req, formData);
        }
    }
}

