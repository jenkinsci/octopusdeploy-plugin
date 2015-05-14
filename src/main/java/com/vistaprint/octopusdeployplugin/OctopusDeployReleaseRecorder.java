package com.vistaprint.octopusdeployplugin;
import hudson.Launcher;
import hudson.Extension;
import hudson.model.*;
import hudson.tasks.*;
import hudson.scm.*;
import java.util.List;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Creates releases
 * @author badriance
 */
public class OctopusDeployReleaseRecorder extends Recorder {

    /**
     * The octopus project
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
     * releaseNotesFromFile
     */
    private final boolean releaseNotesFromFile;
    public boolean getReleaseNotesSource() {
        return releaseNotesFromFile;
    }

    private final String releaseNotesFileSource;
    public String getReleaseNotesFileSource() {
        return releaseNotesFileSource;
    }
    
    private final String packageVersion;
    public String getPackageVersion() {
        return releaseNotesFileSource;
    }
    
    
    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public OctopusDeployReleaseRecorder(String project, String releaseVersion, boolean releaseNotesFromFile, String releaseNotesFileSource, String packageVersion) {
        this.project = project;
        this.releaseVersion = releaseVersion;
        this.packageVersion = packageVersion;
        this.releaseNotesFromFile = releaseNotesFromFile;
        this.releaseNotesFileSource = releaseNotesFileSource;
    }
    
    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        
        try {

            Result result = build.getResult();
            Job  job = build.getParent();
            // need to get all the changesets for all builds from this one up to
            // but not including the last successful build
            List changesets = build.getChangeSets();
            
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
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
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
    }
}

