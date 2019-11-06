package hudson.plugins.octopusdeploy;

import com.octopusdeploy.api.OctopusApi;
import com.octopusdeploy.api.data.Space;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.plugins.octopusdeploy.constants.OctoConstants;
import hudson.plugins.octopusdeploy.utils.Lazy;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.types.Commandline;
import org.apache.xpath.operations.Bool;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkState;

/**
 * The AbstractOctopusDeployRecorder tries to take care of most of the Octopus
 * Deploy server access.
 * @author wbenayed
 */
public abstract class AbstractOctopusDeployRecorder extends Recorder {

    /**
     * Cache for OctopusDeployServer instance used in deployment
     * transient keyword prevents leaking API key to Job configuration
     */
    protected transient Lazy<OctopusDeployServer> lazyOctopusDeployServer;

    public OctopusDeployServer getOctopusDeployServer() {
        return lazyOctopusDeployServer
                .getOrCompute(()->getOctopusDeployServer(getServerId()));
    }

    /**
     * The serverId to use for this deployment
     */
    protected String serverId;
    public String getServerId() {
        return serverId;
    }

    /**
     * The toolId to use for this deployment
     */
    protected String toolId;
    public String getToolId() {return toolId;}

    /**
     * The spaceId to use for this deployment
     */
    protected String spaceId;
    public String getSpaceId() {
        return spaceId;
    }

    public static Boolean hasSpaces() {
        try {
            return getDefaultOctopusDeployServer().getApi().forSystem().getSupportsSpaces();
        } catch (Exception ex) {
            Logger.getLogger(AbstractOctopusDeployRecorder.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
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
     * The variables to use for a deploy to in Octopus.
     */
    protected String variables;
    public String getVariables() {
        return variables;
    }

    /**
     * The Tenant to use for a deploy to in Octopus.
     */
    protected String tenant;
    public String getTenant() {
        return tenant;
    }

    protected String tenantTag;
    public String getTenantTag() {
        return tenantTag;
    }

    /**
     * The additional arguments to pass to octo.exe
     */
    protected String additionalArgs;
    public String getAdditionalArgs() {
        return additionalArgs;
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
     * Whether or not to enable verbose logging
     */
    protected boolean verboseLogging;
    public boolean getVerboseLogging() {
        return verboseLogging;
    }

    /**
     * Specifies maximum time (timespan format) that the console session will wait for
     * the deployment to finish(default 00:10:00)
     */
    protected String deploymentTimeout;
    public String getDeploymentTimeout() {
        return deploymentTimeout;
    }

    /**
     * Whether to cancel the deployment if the deployment timeout is reached
     */
    protected boolean cancelOnTimeout;
    public boolean getCancelOnTimeout() {
        return cancelOnTimeout;
    }

    /**
     * Get the default OctopusDeployServer from OctopusDeployPlugin configuration
     * @return the default server
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
     * @return all configured servers
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

    public static OctoInstallation[] getOctopusToolInstallations() {
        OctoInstallation.DescriptorImpl descriptor = (OctoInstallation.DescriptorImpl) Jenkins.getInstance().getDescriptor(OctoInstallation.class);
        return descriptor.getInstallations();
    }

    public static List<String> getOctopusToolIds() {
        List<String> ids = new ArrayList<>();
        for (OctoInstallation i : getOctopusToolInstallations()) {
            ids.add(i.getName());
        }
        return ids;
    }

    public static String getOctopusToolPath(String name) {
        OctoInstallation.DescriptorImpl descriptor = (OctoInstallation.DescriptorImpl) Jenkins.getInstance().getDescriptor(OctoInstallation.class);
        return descriptor.getInstallation(name).getPathToOctoExe();
    }

    /**
     * Get the instance of OctopusDeployServer by serverId
     * @param serverId The id of OctopusDeployServer in the configuration.
     * @return the server by id
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

    public Boolean hasAdvancedOptions() {
        return getVerboseLogging() || (getAdditionalArgs() != null && !getAdditionalArgs().isEmpty());
    }

    /**
     * Get OctopusApi instance for this deployment
     * @return the api for a given server
     */
    public OctopusApi getApi() {
        return getOctopusDeployServer().getApi();
    }

    List<String> getCommonCommandArguments() {
        List<String> commands = new ArrayList<>();

        OctopusDeployServer server = getOctopusDeployServer(this.serverId);
        String serverUrl = server.getUrl();
        String apiKey = server.getApiKey().getPlainText();
        boolean ignoreSslErrors = server.getIgnoreSslErrors();

        checkState(StringUtils.isNotBlank(serverUrl), String.format(OctoConstants.Errors.INPUT_CANNOT_BE_BLANK_MESSAGE_FORMAT, "Octopus URL"));
        checkState(StringUtils.isNotBlank(apiKey), String.format(OctoConstants.Errors.INPUT_CANNOT_BE_BLANK_MESSAGE_FORMAT, "API Key"));

        commands.add(OctoConstants.Commands.Arguments.PROJECT_NAME);
        commands.add(project);

        commands.add(OctoConstants.Commands.Arguments.SERVER_URL);
        commands.add(serverUrl);
        commands.add(OctoConstants.Commands.Arguments.API_KEY);
        commands.add(apiKey);
        if (StringUtils.isNotBlank(spaceId)) {
            commands.add(OctoConstants.Commands.Arguments.SPACE_NAME);
            commands.add(spaceId);
        }

        if (waitForDeployment) {
            if (StringUtils.isNotBlank(deploymentTimeout)) {
                checkState(OctopusValidator.isValidTimeSpan(deploymentTimeout), String.format(OctoConstants.Errors.INPUT_IS_INVALID_MESSAGE_FORMAT, "Deployment Timeout (expects format:\"HH:mm:ss\")"));
                commands.add("--deploymentTimeout");
                commands.add(deploymentTimeout);
            }

            if (cancelOnTimeout) {
                commands.add("--cancelOnTimeout");
            }
        }

        if (ignoreSslErrors) {
            commands.add("--ignoreSslErrors");
        }

        if (verboseLogging) {
            commands.add("--debug");
        }

        if(StringUtils.isNotBlank(additionalArgs)) {
            final String[] myArgs = Commandline.translateCommandline(additionalArgs);
            commands.addAll(Arrays.asList(myArgs));
        }

        return commands;
    }

    Boolean[] getMasks(List<String> commands, String... commandArgumentsToMask) {
        final Boolean[] masks = new Boolean[commands.size()];
        Arrays.fill(masks, Boolean.FALSE);
        for(String commandArgumentToMask : commandArgumentsToMask) {
            if(commands.contains(commandArgumentToMask)) {
                masks[commands.indexOf(commandArgumentToMask) + 1] = Boolean.TRUE;
            }
        }
        return masks;
    }

    public Result launchOcto(Launcher launcher, List<String> commands, Boolean[] masks, EnvVars environment, BuildListener listener) {
        Log log = new Log(listener);
        int exitCode = -1;
        final String octopusCli = this.getToolId();

        checkState(StringUtils.isNotBlank(octopusCli), String.format(OctoConstants.Errors.INPUT_CANNOT_BE_BLANK_MESSAGE_FORMAT, "Octopus CLI"));

        final String cliPath = getOctopusToolPath(octopusCli);
        if(StringUtils.isNotBlank(cliPath) && new File(cliPath).exists()) {
            final List<String> cmdArgs = new ArrayList<>();
            final List<Boolean> cmdMasks = new ArrayList<>();

            cmdArgs.add(cliPath);
            cmdArgs.addAll(commands);

            cmdMasks.add(Boolean.FALSE);
            cmdMasks.addAll(Arrays.asList(masks));

            Proc process = null;
            try {
                //environment.put("OCTOEXTENSION", getClass().getPackage().getImplementationVersion());
                environment.put("OCTOEXTENSION", "");
                process = launcher
                        .launch()
                        .cmds(cmdArgs)
                        .masks(ArrayUtils.toPrimitive(cmdMasks.toArray((Boolean[])Array.newInstance(Boolean.class, 0))))
                        .stdout(listener)
                        .envs(environment)
                        .pwd(environment.get("WORKSPACE", ""))
                        .start();

                exitCode = process.join();

                log.info(String.format("Octo.exe exit code: %d", exitCode));

            } catch (IOException e) {
                final String message = "Error from Octo.exe: " + e.getMessage();
                log.error(message);
                return Result.FAILURE;
            } catch (InterruptedException e) {
                final String message = "Unable to wait for Octo.exe: " + e.getMessage();
                log.error(message);
                return Result.FAILURE;
            }

            if(exitCode == 0)
                return Result.SUCCESS;

            log.error("Unable to create or deploy release. Please check the build log for details on the error.");
            return Result.FAILURE;
        }

        log.error("OCTOPUS-JENKINS-INPUT-ERROR-0003: The path of \"" + cliPath + "\" for the selected Octopus CLI does not exist.");
        return Result.FAILURE;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public static abstract class AbstractOctopusDeployDescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
            save();
            return true;
        }

        protected OctopusApi getApiByServerId(String serverId){
            return AbstractOctopusDeployRecorder.getOctopusDeployServer(serverId).getApi();
        }

        public String getDefaultOctopusDeployServerId() {
            OctopusDeployServer server = AbstractOctopusDeployRecorder.getDefaultOctopusDeployServer();
            if(server != null){
                return server.getId();
            }
            return null;
        }

        public String getDefaultOctopusToolId() {
            OctoInstallation tool = OctoInstallation.getDefaultInstallation();
            if (tool != null) {
                return tool.getName();
            }
            return null;
        }

        /**
         * Check that the serverId field is not empty and does exist.
         * @param serverId The id of OctopusDeployServer in the configuration.
         * @return Ok if not empty, error otherwise.
         */
        public FormValidation doCheckServerId(@QueryParameter String serverId) {
            serverId = serverId.trim();
            return OctopusValidator.validateServerId(serverId);
        }

        /**
         * Data binding that returns all configured Octopus server ids to be used in the serverId drop-down list.
         * @return ComboBoxModel
         */
        public ComboBoxModel doFillServerIdItems() {
            return new ComboBoxModel(getOctopusDeployServersIds());
        }

        public ComboBoxModel doFillToolIdItems() {
            return new ComboBoxModel(getOctopusToolIds());
        }

        public ListBoxModel doFillSpaceIdItems(@QueryParameter String serverId) {
            ListBoxModel spaceItems = new ListBoxModel();
            if(doCheckServerId(serverId).kind != FormValidation.Kind.OK) {
                return spaceItems;
            }

            OctopusApi api = getApiByServerId(serverId).forSystem();
            try {
                Set<Space> spaces = api.getSpacesApi().getAllSpaces();
                spaceItems.add("", "");
                for (Space space : spaces) {
                    spaceItems.add(space.getName(), space.getId());
                }
            } catch (Exception ex) {
                Logger.getLogger(AbstractOctopusDeployRecorder.class.getName()).log(Level.SEVERE, null, ex);
            }

            return spaceItems;
        }

    }
}
