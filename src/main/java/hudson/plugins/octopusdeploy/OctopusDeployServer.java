package hudson.plugins.octopusdeploy;


import com.octopusdeploy.api.OctopusApi;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;

/**
 * @author wbenayed
 */

public class OctopusDeployServer implements Serializable {

    private final boolean isDefault;
    public boolean isDefault() {
        return isDefault;
    }

    private String id;
    public String getId() {
        return id;
    }

    private String url;
    public String getUrl() {
        return url;
    }

    private String apiKey;
    public String getApiKey() {
        return apiKey;
    }

    private transient OctopusApi api;
    public OctopusApi getApi() {
        ///TODO use better approach to achieve Laziness
        if (api == null) {
            api = new OctopusApi(url,apiKey);
        }
        return api;
    }

    public OctopusDeployServer(String serverId, String url, String apiKey, boolean isDefault) {
        this.id = serverId;
        this.url = url;
        this.apiKey = apiKey;
        this.isDefault = isDefault;
    }

    @DataBoundConstructor
    public OctopusDeployServer(String serverId, String url, String apiKey) {
        this(serverId,url,apiKey,false);
    }
}
