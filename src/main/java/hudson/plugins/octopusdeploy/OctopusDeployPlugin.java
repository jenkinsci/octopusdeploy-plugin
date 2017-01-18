package hudson.plugins.octopusdeploy;

import jenkins.model.*;
import org.kohsuke.stapler.*;
import net.sf.json.JSONObject;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import java.io.IOException;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This plugin is only responsible for containing global configuration information
 * to be used by the ReleaseRecorder and DeploymentRecorder.
 * @author badriance
 */
public class OctopusDeployPlugin extends GlobalPluginConfiguration {

    @Extension
    public static final class DescriptorImpl extends Descriptor<GlobalConfiguration> {

        private String apiKey;
        public String getApiKey() {
            return apiKey;
        }
        
        private String octopusHost;
        public String getOctopusHost() {
            return octopusHost;
        }
        
        public DescriptorImpl() {
            load();
        }
        
        @Override
        public String getDisplayName() {
            return "OctopusDeploy Plugin";
        }

        /**
         * Validate that the host is:
         *  - Not empty
         *  - A well formed URL
         *  - A real location that we can connect to
         * @param octopusHost the host URL for the octopus deploy instance
         * @return Form validation to present on the Jenkins UI
         */
        public FormValidation doCheckOctopusHost(@QueryParameter String octopusHost) {
            if (octopusHost.isEmpty()) {
                return FormValidation.warning("Please enter a url to your OctopusDeploy Host");
            }
            URL url;
            try {
                url = new URL(octopusHost);
            } catch (MalformedURLException ex) {
                final String INVALID_URL = "Supplied Octopus Host URL is invalid";
                Logger.getLogger(OctopusDeployPlugin.class.getName()).log(Level.WARNING, INVALID_URL, ex);
                return FormValidation.error(INVALID_URL);
            }

            try {
                URLConnection connection = url.openConnection();
                if (connection instanceof HttpURLConnection) {
                    HttpURLConnection httpConnection = (HttpURLConnection)connection;
                    httpConnection.setRequestMethod("HEAD");
                    int code = httpConnection.getResponseCode();
                    httpConnection.disconnect();
                    if (code >= 400)
                    {
                        return FormValidation.error("Could not connect. HTTP Response %s", code);
                    }
                }
            } catch (IOException ex) {
                final String UNABLE_TO_CONNECT = "Unable to connect to Octopus Host URL";
                Logger.getLogger(OctopusDeployPlugin.class.getName()).log(Level.WARNING, UNABLE_TO_CONNECT, ex);
                return FormValidation.error("%s - %s", UNABLE_TO_CONNECT, ex.getMessage());
            }

            return FormValidation.ok();
        }
        
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
            JSONObject json = formData.getJSONObject("octopusConfig");
            apiKey = json.getString("apiKey");
            octopusHost = json.getString("octopusHost");
            
            save();
            // update the other plugin components when this changes!
            Jenkins jenkinsInstance = Jenkins.getInstance();
            if (jenkinsInstance != null) {
                OctopusDeployReleaseRecorder.DescriptorImpl releaseDescriptor = (OctopusDeployReleaseRecorder.DescriptorImpl) 
                        jenkinsInstance.getDescriptor(OctopusDeployReleaseRecorder.class);
                OctopusDeployDeploymentRecorder.DescriptorImpl deployDescriptor = (OctopusDeployDeploymentRecorder.DescriptorImpl) 
                        jenkinsInstance.getDescriptor(OctopusDeployDeploymentRecorder.class);
                releaseDescriptor.updateGlobalConfiguration();
                deployDescriptor.updateGlobalConfiguration();
            }
            return super.configure(req, formData);
        }
    }
}
