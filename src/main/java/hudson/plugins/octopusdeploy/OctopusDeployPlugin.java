package hudson.plugins.octopusdeployplugin;

import jenkins.model.*;
import org.kohsuke.stapler.*;
import net.sf.json.JSONObject;
import hudson.Extension;
import hudson.model.Descriptor;

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
