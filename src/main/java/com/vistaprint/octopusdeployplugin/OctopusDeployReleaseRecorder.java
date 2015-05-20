package com.vistaprint.octopusdeployplugin;
import hudson.Launcher;
import hudson.Extension;
import hudson.model.*;
import hudson.tasks.*;
import hudson.scm.*;
import hudson.util.*;
import java.io.IOException;
import java.util.List;
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
    
    private String octopusHost;
    private String apiKey;
    
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
         //   SetGlobalConfiguration();
            
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
     * Loads the OctopusDeployPlugin descriptor and pulls configuration from it
     * for API Key, and Host.
     */
    private void SetGlobalConfiguration() {
        OctopusDeployPlugin.DescriptorImpl descriptor = (OctopusDeployPlugin.DescriptorImpl) 
                    Jenkins.getInstance().getDescriptor( OctopusDeployPlugin.class );
        apiKey = descriptor.getApiKey();
        octopusHost = descriptor.getOctopusHost();
    }
    
    /**
     * Descriptor for {@link OctopusDeployReleaseRecorder}. Used as a singleton.
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
            return "OctopusDeploy Release";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
            save();
            return super.configure(req, formData);
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
    }
}

