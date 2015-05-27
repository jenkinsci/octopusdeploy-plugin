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

    
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends Descriptor<GlobalConfiguration> {

        private String apiKey;
        public String getApiKey() {
            return apiKey;
        }
        
        private String octopusHost;
        public String getOctopusHost() {
            return octopusHost;
        }
        
        @Override
        public String getDisplayName() {
            return "OctopusDeploy Plugin";
        }

        public FormValidation doCheckOctopusHost(@QueryParameter String octopusHost) {
            if ("".equals(octopusHost)) {
                return FormValidation.warning("Please enter a url to your OctopusDeploy Host");
            }
            URL url;
            try {
                url = new URL(octopusHost);
            } catch (MalformedURLException ex) {
                final String INVALID_URL = "Supplied Octopus Host URL is invalid";
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
            return super.configure(req, formData);
        }
    }
}
