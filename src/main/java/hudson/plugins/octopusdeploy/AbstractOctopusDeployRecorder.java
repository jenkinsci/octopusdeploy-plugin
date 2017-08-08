package hudson.plugins.octopusdeploy;

import com.octopusdeploy.api.OctopusApi;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;
import jenkins.model.Jenkins;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wbenayed
 */
public abstract class AbstractOctopusDeployRecorder extends Recorder {

    /**
     * Cache for OctopusDeployServer instance used in deployment
     * transient keyword prevents leaking API key to Job configuration
     */
    protected transient OctopusDeployServer octopusDeployServer;

    public OctopusDeployServer getOctopusDeployServer() {
        ///TODO use better approach to achieve Laziness
        if (octopusDeployServer == null) {
            octopusDeployServer = getOctopusDeployServer(getServerId());
        }
        return octopusDeployServer;
    }

    /**
     * The serverId to use for this deployment
     */
    protected String serverId;
    public String getServerId() {
        return serverId;
    }

    /**
     * The project name as defined in Octopus.
     */
    protected String project;
    public String getProject() {
        return project;
    }

    /**
     * The environment to deploy to, if we are deploying.
     */
    protected String environment;
    public String getEnvironment() {
        return environment;
    }

    /**
     * The Tenant to use for a deploy to in Octopus.
     */
    protected String tenant;
    public String getTenant() {
        return tenant;
    }

    /**
     * Whether or not perform will return control immediately, or wait until the Deployment
     * task is completed.
     */
    protected boolean waitForDeployment;
    public boolean getWaitForDeployment() {
        return waitForDeployment;
    }


    /**
     * Get the default OctopusDeployServer from OctopusDeployPlugin configuration
     * */
    protected static OctopusDeployServer getDefaultOctopusDeployServer() {
        Jenkins jenkinsInstance = Jenkins.getInstance();
        if (jenkinsInstance == null) {
            throw new IllegalStateException("Jenkins instance is null");
        }
        OctopusDeployPlugin.DescriptorImpl descriptor = (OctopusDeployPlugin.DescriptorImpl) jenkinsInstance.getDescriptor(OctopusDeployPlugin.class);
        return descriptor.getDefaultOctopusDeployServer();
    }

    /**
     * Get the list of OctopusDeployServer from OctopusDeployPlugin configuration
     * */
    public static List<OctopusDeployServer> getOctopusDeployServers() {
        Jenkins jenkinsInstance = Jenkins.getInstance();
        if (jenkinsInstance == null) {
            throw new IllegalStateException("Jenkins instance is null");
        }
        OctopusDeployPlugin.DescriptorImpl descriptor = (OctopusDeployPlugin.DescriptorImpl) jenkinsInstance.getDescriptor(OctopusDeployPlugin.class);
        return descriptor.getOctopusDeployServers();
    }


    public static List<String> getOctopusDeployServersIds() {

        List<String> ids = new ArrayList<>();
        for (OctopusDeployServer s:getOctopusDeployServers()) {
            ids.add(s.getId());
        }
        return ids;
    }

    /**
     * Get the instance of OctopusDeployServer by serverId
     * */
    public static OctopusDeployServer getOctopusDeployServer(String serverId) {
        if (serverId == null || serverId.isEmpty()){
            return getDefaultOctopusDeployServer();
        }
        for(OctopusDeployServer server : getOctopusDeployServers()) {
            if(server.getId().equals(serverId)) {
                return server;
            }
        }
        return null;
    }


    /**
     * Get OctopusApi instance for this deployment
     */
    public OctopusApi getApi() {
        return getOctopusDeployServer().getApi();
    }


    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }
}
