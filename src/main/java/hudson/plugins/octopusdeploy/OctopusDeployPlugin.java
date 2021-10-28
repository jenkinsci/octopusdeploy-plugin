package hudson.plugins.octopusdeploy;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.plugins.octopusdeploy.utils.JenkinsHelpers;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import jenkins.model.GlobalConfiguration;
import jenkins.model.GlobalPluginConfiguration;
import net.sf.json.JSONObject;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;
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

import static hudson.plugins.octopusdeploy.services.StringUtil.sanitizeValue;

/**
 * This plugin is only responsible for containing global configuration information
 * to be used by the ReleaseRecorder and DeploymentRecorder.
 * @author badriance
 */
public class OctopusDeployPlugin extends GlobalPluginConfiguration {

    @Extension
    @Symbol("octopusGlobalConfiguration")
    public static final class DescriptorImpl extends Descriptor<GlobalConfiguration> {

        private transient String apiKey;

        private transient String octopusHost;

        private List<OctopusDeployServer> octopusDeployServers;

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

        public List<OctopusDeployServer> getOctopusDeployServers() {
            if (octopusDeployServers != null) {
                return octopusDeployServers;
            }
            return Collections.emptyList();
        }

        @DataBoundSetter
        public void setOctopusDeployServers(List<OctopusDeployServer> servers) {
            octopusDeployServers = servers;
            save();
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
                OctopusDeployServer server = new OctopusDeployServer("default", octopusHost, Secret.fromString(apiKey), true, false);
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
         * @param serverId the uniqueId for an Octopus Deploy instance
         * @param url the url of the Octopus Deploy server
         * @param apiKey the api key of the Octopus Deploy server
         * @return Form validation to present on the Jenkins UI
         */
        public FormValidation doCheckServerId(@QueryParameter String serverId,@QueryParameter String url,@QueryParameter String apiKey) {
            serverId = sanitizeValue(serverId);
            if (serverId.isEmpty()) {
                return FormValidation.warning("Please set a Server Id");
            }
            for (OctopusDeployServer s:getOctopusDeployServers()){
                boolean serverIdMatches = serverId.equals(s.getServerId());
                boolean urlsDiffer = !url.equals(s.getUrl());
                boolean apiKeysDiffer = !apiKey.equals(s.getApiKey().getEncryptedValue());
                // this validation function fires when serverId OR url OR apiKey change, which is documented (poorly) - https://wiki.jenkins.io/display/JENKINS/Form+Validation
                // if 1 field is only changing we can consider it an update
                // but if the 2 fields have changed, we struggle to tell it apart from a complete change versus a new addition
                if (serverIdMatches && urlsDiffer && apiKeysDiffer){
                    return FormValidation.warning("The Server Id you entered may already exist. If you are updating both the API key and URL of an existing server entry you can disregard this warning.");
                }
                // to solve this better we would need an identifier that the user cannot edit to be linked to these fields
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
            JenkinsHelpers.getJenkins().checkPermission(Jenkins.ADMINISTER);
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
            apiKey = sanitizeValue(apiKey);
            if (apiKey.isEmpty()) {
                return FormValidation.warning("Please set a API Key generated from Octopus Deploy Server.");
            }

            try {
                if (!isApiKeyValid(apiKey) && !isApiKeyValid(Secret.decrypt(apiKey).getPlainText())) {
                    return FormValidation.error("Supplied Octopus API Key format is invalid. It should look like API-XXXXXXXXXXXXXXXXXXXXXXXXXXX");
                }
            } catch (NullPointerException ex) {
                return FormValidation.error("Supplied Octopus API Key format is invalid. It should look like API-XXXXXXXXXXXXXXXXXXXXXXXXXXX");
            }
            return FormValidation.ok();
        }

        private static boolean isApiKeyValid(String apiKeyValue) {
            final String apiKeyPrefix = "API-";
            if (!apiKeyValue.startsWith(apiKeyPrefix)) {
                return false;
            }

            int keyLength = apiKeyValue.length();
            if (keyLength < 29 || keyLength > 36) {
                return false;
            }

            String keyWithoutPrefix = apiKeyValue.substring(apiKeyPrefix.length());
            for (char c : keyWithoutPrefix.toCharArray()) {
                if (!Character.isDigit(c) && !Character.isUpperCase(c)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
            List<OctopusDeployServer> servers = null;
            JSONObject json = formData.getJSONObject("octopusConfig");

            if (!json.isEmpty()) {
                servers = req.bindJSONToList(OctopusDeployServer.class, json.get("servers"));
            }
            setOctopusDeployServers(servers);

            return super.configure(req, formData);
        }
    }
}
