package hudson.plugins.octopusdeploy;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.model.GlobalConfiguration;
import jenkins.model.GlobalPluginConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
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

        private transient String apiKey;

        private transient String octopusHost;

        /**
         * Get the default OctopusDeployServer instance
         * @return the default server
         */
        public OctopusDeployServer getDefaultOctopusDeployServer() {

            for (OctopusDeployServer s : getOctopusDeployServers()) {
                if (s.isDefault()) {
                    return s;
                }
            }
            if(!getOctopusDeployServers().isEmpty()) {
                return getOctopusDeployServers().get(0);
            }
            return null;
        }

        private List<OctopusDeployServer> octopusDeployServers;
        public List<OctopusDeployServer> getOctopusDeployServers() {
            if (octopusDeployServers != null) {
                return octopusDeployServers;
            }
            return Collections.emptyList();
        }

        private void setOctopusDeployServers(List<OctopusDeployServer> servers) {
            octopusDeployServers = servers;
        }

        public DescriptorImpl() {
            load();
            loadLegacyOctopusDeployServerConfig();
        }

        /**
         * Load legacy OctopusPlugin configuration format
         */
		@SuppressFBWarnings(value = "UWF_UNWRITTEN_FIELD", justification = "This is for backwards compatiblity on Jenkins plugin upgrade" )
        private void loadLegacyOctopusDeployServerConfig() {
            if (doesLegacyOctopusDeployServerExist()){
                OctopusDeployServer server = new OctopusDeployServer("default", octopusHost, apiKey, true);
                if(octopusDeployServers == null)
                {
                    octopusDeployServers = new ArrayList<>();
                }
                octopusDeployServers.add(0, server);
            }
        }

        private boolean doesLegacyOctopusDeployServerExist() {
            return octopusHost != null && apiKey !=null;
        }

        @Override
        public String getDisplayName() {
            return "OctopusDeploy Plugin";
        }

        /**
         * Validate that serverId is:
         *  - Not empty
         *  - Unique
         * @param serverId the uniqueId for an octopus deploy instance
         * @return Form validation to present on the Jenkins UI
         */
        public FormValidation doCheckServerId(@QueryParameter String serverId,@QueryParameter String url,@QueryParameter String apiKey) {
            serverId = serverId.trim();
            if (serverId.isEmpty()) {
                return FormValidation.warning("Please set a ServerID");
            }
            for (OctopusDeployServer s:getOctopusDeployServers()){
                if (serverId.equals(s.getId()) && !url.equals(s.getUrl()) && !apiKey.equals(s.getApiKey())){
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
        @RequirePOST
        public FormValidation doCheckUrl(@QueryParameter String url) {
            if (url.isEmpty()) {
                return FormValidation.warning("Please enter a url to your OctopusDeploy Host");
            }
            Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
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
         * @param apiKey Octopus API Key used for deployment
         * @return Form validation to present on the Jenkins UI
         */
        public FormValidation doCheckApiKey(@QueryParameter String apiKey) {
            apiKey = apiKey.trim();
            if (apiKey.isEmpty()) {
                return FormValidation.warning("Please set a API Key generated from OctopusDeploy Server.");
            }
            if (!apiKey.matches("API\\-\\w{25,27}")) {
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
