package hudson.plugins.octopusdeploy;


import com.octopusdeploy.api.OctopusApi;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import org.kohsuke.stapler.DataBoundConstructor;
import java.io.Serializable;

/**
 * @author wbenayed
 */

public class OctopusDeployServer implements Serializable {

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

    @XStreamOmitField
    private OctopusApi api;
    public OctopusApi getApi() {
        ///TODO use better approach to achieve Laziness
        if (api == null) {
            api = new OctopusApi(url,apiKey);
        }
        return api;
    }

    @DataBoundConstructor
    public OctopusDeployServer(String serverId, String url, String apiKey) {
        this.id = serverId;
        this.url = url;
        this.apiKey = apiKey;
    }
}
