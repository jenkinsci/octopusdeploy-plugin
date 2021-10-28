package hudson.plugins.octopusdeploy;

import com.octopusdeploy.api.OctopusApi;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;

import static hudson.plugins.octopusdeploy.services.StringUtil.sanitizeValue;

/**
 * @author wbenayed
 */
public class OctopusDeployServer implements Serializable {
    // This value should be incremented every time that this serializable's contract changes
    private static final long serialVersionUID = 1;
            
    private final boolean isDefault;
    public boolean isDefault() {
        return isDefault;
    }

    private String serverId;
    public String getServerId() {
        return serverId;
    }

    private String url;
    public String getUrl() {
        return url;
    }

    private Secret apiKey;
    public Secret getApiKey() {
        return apiKey;
    }

    private boolean ignoreSslErrors;
    public boolean getIgnoreSslErrors() {
        return ignoreSslErrors;
    }

    private transient OctopusApi api;
    public OctopusApi getApi() {
        ///TODO use better approach to achieve Laziness
        if (api == null) {
            api = new OctopusApi(url, apiKey.getPlainText());
        }
        return api;
    }

    public OctopusDeployServer(String serverId, String url, Secret apiKey, boolean isDefault, boolean ignoreSslErrors) {
        this.serverId = sanitizeValue(serverId);
        this.url = sanitizeValue(url);
        this.apiKey = apiKey;
        this.isDefault = isDefault;
        this.ignoreSslErrors = ignoreSslErrors;
    }

    @DataBoundConstructor
    public OctopusDeployServer(String serverId, String url, String apiKey, boolean ignoreSslErrors) {
        this(serverId, url, Secret.fromString(apiKey), false, ignoreSslErrors);
    }
}
