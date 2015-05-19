package com.vistaprint.octopusdeployplugin;
import hudson.Launcher;
import hudson.Extension;
import hudson.model.*;
import hudson.tasks.*;
import hudson.scm.*;
import java.util.List;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

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
            SetGlobalConfiguration();
            
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
}

