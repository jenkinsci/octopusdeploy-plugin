package hudson.plugins.octopusdeploy;

import jenkins.model.*;
import org.kohsuke.stapler.*;
import net.sf.json.JSONObject;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import java.io.IOException;
import java.net.*;
import java.util.List;
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

        private List<OctopusDeployServer> octopusDeployServers;

        public List<OctopusDeployServer> getOctopusDeployServers() {
            return octopusDeployServers;
        }

        private void setOctopusDeployServers(List<OctopusDeployServer> servers) {
            octopusDeployServers = servers;
        }

        public DescriptorImpl() {
            load();
        }
        
        @Override
        public String getDisplayName() {
            return "OctopusDeploy Plugin";
        }

        /**
         * Validate that serverId is:
         *  - Unique
         * @param serverId the uniqueId for an octopus deploy instance
         * @param url the host URL for an octopus deploy instance
         * @return Form validation to present on the Jenkins UI
         */
        public FormValidation doCheckServerId(@QueryParameter String serverId, @QueryParameter String url) {
            serverId = serverId.trim();

            for (OctopusDeployServer s:octopusDeployServers){
                if (serverId.equals(s.getId()) && !url.equals(s.getUrl())){
                    return FormValidation.error("The Server ID you entered already exists.");
                }
            }

            return FormValidation.ok();
        }

        /**
         * Validate that the host is:
         *  - Not empty
         *  - A well formed URL
         *  - A real location that we can connect to
         * @param url the host URL for the octopus deploy instance
         * @return Form validation to present on the Jenkins UI
         */
        public FormValidation doCheckUrl(@QueryParameter String url) {
            if (url.isEmpty()) {
                return FormValidation.warning("Please enter a url to your OctopusDeploy Host");
            }

            try {

                URLConnection connection = new URL(url).openConnection();
                if (connection instanceof HttpURLConnection) {
                    HttpURLConnection httpConnection = (HttpURLConnection) connection;
                    httpConnection.setRequestMethod("HEAD");
                    int code = httpConnection.getResponseCode();
                    httpConnection.disconnect();
                    if (code >= 400) {
                        return FormValidation.error("Could not connect. HTTP Response %s", code);
                    }
                }
            }
            catch (MalformedURLException ex) {
                final String INVALID_URL = "Supplied Octopus Host URL is invalid";
                Logger.getLogger(OctopusDeployPlugin.class.getName()).log(Level.WARNING, INVALID_URL, ex);
                return FormValidation.error(INVALID_URL);
            }
            catch (IOException ex) {
                final String UNABLE_TO_CONNECT = "Unable to connect to Octopus Host URL";
                Logger.getLogger(OctopusDeployPlugin.class.getName()).log(Level.WARNING, UNABLE_TO_CONNECT, ex);
                return FormValidation.error("%s - %s", UNABLE_TO_CONNECT, ex.getMessage());
            }

            return FormValidation.ok();
        }

        /**
         * Validate that the apiKey is:
         *  - Not empty
         *  - has a valid OctopusDeploy API Key format: API-XXXXXXXXX
         *  - A real location that we can connect to
         * @param apiKey Octopus API Key used for deployment
         * @return Form validation to present on the Jenkins UI
         */
        public FormValidation doCheckApiKey(@QueryParameter String apiKey) {
            apiKey = apiKey.trim();
            if (apiKey.isEmpty()) {
                return FormValidation.warning("Please set a API Key generated from OctopusDeploy Server.");
            }
            if (!apiKey.matches("API\\-\\w{27}")) {
                return FormValidation.error("Supplied Octopus API Key format is invalid. It should look like API-XXXXXXXXXXXXXXXXXXXXXXXXXXX");
            }
            return FormValidation.ok();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
            List<OctopusDeployServer> servers = null;
            JSONObject json = formData.getJSONObject("octopusConfig");

            if (!json.isEmpty()) {
                servers = req.bindJSONToList(OctopusDeployServer.class, json.get("servers"));
            }
            setOctopusDeployServers(servers);
            
            save();
            return super.configure(req, formData);
        }
    }
}
