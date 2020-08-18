package hudson.plugins.octopusdeploy;

import com.google.common.base.Splitter;
import com.octopusdeploy.api.OctopusApi;
import com.octopusdeploy.api.data.Environment;
import com.octopusdeploy.api.data.Project;
import com.octopusdeploy.api.data.*;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.octopusdeploy.constants.OctoConstants;
import hudson.remoting.VirtualChannel;
import hudson.scm.ChangeLogSet;
import hudson.tasks.BuildStepMonitor;
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.VariableResolver;
import jenkins.util.BuildListenerAdapter;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.remoting.RoleChecker;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static hudson.plugins.octopusdeploy.services.StringUtil.sanitizeValue;

/**
 * Creates a release and optionally deploys it.
 */
public class OctopusDeployReleaseRecorder extends AbstractOctopusDeployRecorderPostBuildStep implements Serializable {
    /**
     * The release version as defined in Octopus.
     */
    private final String releaseVersion;
    public String getReleaseVersion() {
        return releaseVersion;
    }

    /**
     * Are there release notes for this release?
     */
    private boolean releaseNotes;
    public boolean getReleaseNotes() {
        return releaseNotes;
    }

    @DataBoundSetter
    public void setReleaseNotes(boolean releaseNotes) {
        this.releaseNotes = releaseNotes;
    }

    /**
     * Where are the release notes located?
     */
    private String releaseNotesSource;
    public String getReleaseNotesSource() {
        return releaseNotesSource;
    }

    public boolean isReleaseNotesSourceFile() {
        return "file".equals(releaseNotesSource);
    }

    public boolean isReleaseNotesSourceScm() {
        return "scm".equals(releaseNotesSource);
    }
    
    private String channel;
    public String getChannel() {
        return channel;
    }

    /**
     * Write a link back to the originating Jenkins build to the
     * Octopus release notes?
     */
    private boolean releaseNotesJenkinsLinkback;
    public boolean getJenkinsUrlLinkback() {
        return releaseNotesJenkinsLinkback;
    }

    @DataBoundSetter
    public void setJenkinsUrlLinkback(boolean jenkinsUrlLinkback) {
        this.releaseNotesJenkinsLinkback = jenkinsUrlLinkback;
    }


    /**
     * The file that the release notes are in.
     */
    private String releaseNotesFile;
    public String getReleaseNotesFile() {
        return releaseNotesFile;
    }

    @DataBoundSetter
    public void setReleaseNotesFile(String releaseNotesFile) { this.releaseNotesFile = sanitizeValue(releaseNotesFile); }

    /**
     * Should this release be deployed right after it is created?
     */
    private boolean deployThisRelease;
    @Exported
    public boolean getDeployThisRelease() {
        return deployThisRelease;
    }

    /**
     * All packages needed to create this new release.
     */
    private List<PackageConfiguration> packageConfigs;
    @Exported
    public List<PackageConfiguration> getPackageConfigs() {
        return packageConfigs;
    }

    /**
     * Default package version to use for required packages that are not
     * specified in the Package Configurations
     */
    private String defaultPackageVersion;
    @Exported
    public String getDefaultPackageVersion() {
        return defaultPackageVersion;
    }

    @DataBoundSetter
    public void setDefaultPackageVersion(String defaultPackageVersion) {
        this.defaultPackageVersion = sanitizeValue(defaultPackageVersion);
    }

    @DataBoundSetter
    public void setReleaseNotesSource(String releaseNotesSource) {
        this.releaseNotesSource = sanitizeValue(releaseNotesSource);
    }

    @DataBoundSetter
    public void setPackageConfigs(List<PackageConfiguration> packageConfigs) {
        this.packageConfigs = packageConfigs;
    }

    @DataBoundSetter
    public void setAdditionalArgs(String addtionalArgs) { this.additionalArgs = sanitizeValue(addtionalArgs); }

    public String getAdditionalArgs() {
        return this.additionalArgs;
    }

    @DataBoundSetter
    public void setSpaceId(String spaceId) {
        this.spaceId = sanitizeValue(spaceId);
    }

    public String getSpaceId() {
        return this.spaceId;
    }

    @DataBoundSetter
    public void setChannel(String channel) {
        this.channel = sanitizeValue(channel);
    }

    @DataBoundSetter
    public void setDeployThisRelease(boolean deployThisRelease) {
        this.deployThisRelease = deployThisRelease;
    }


    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public OctopusDeployReleaseRecorder(String serverId, String toolId, String project, String releaseVersion, String spaceId) {

        this.serverId = sanitizeValue(serverId);
        this.toolId = sanitizeValue(toolId);
        this.project = sanitizeValue(project);
        this.releaseVersion = sanitizeValue(releaseVersion);
        this.spaceId = sanitizeValue(spaceId);

        this.releaseNotes = false;
        this.verboseLogging = false;
        this.channel = "Default";
        this.deployThisRelease = false;
        this.cancelOnTimeout = false;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) {
        BuildListenerAdapter listenerAdapter = new BuildListenerAdapter(listener);
        Log log = new Log(listenerAdapter);

        boolean success = true;

        if (Result.FAILURE.equals(run.getResult())) {
            log.info("Not creating a release due to job being in FAILED state.");
            return;
        }

        if (this.deployThisRelease && isNullOrEmpty(this.environment)) {
            log.error("Must provide the environment when deploying the release.");
            run.setResult(Result.FAILURE);
            return;
        }

        EnvVars envVars;
        try {
            envVars = run.getEnvironment(listener);
        } catch (Exception ex) {
            log.fatal(String.format("Failed to retrieve environment variables for this project '%s' - '%s'",
                project, ex.getMessage()));
            run.setResult(Result.FAILURE);
            return ;
        }
        VariableResolver resolver =  new VariableResolver.ByMap<>(envVars);
        EnvironmentVariableValueInjector envInjector = new EnvironmentVariableValueInjector(resolver, envVars);

        // NOTE: hiding the member variables of the same name with their env-injected equivalents
        String project = envInjector.injectEnvironmentVariableValues(this.project);
        String releaseVersion = envInjector.injectEnvironmentVariableValues(this.releaseVersion);
        String releaseNotesFile = envInjector.injectEnvironmentVariableValues(this.releaseNotesFile);
        String environment = envInjector.injectEnvironmentVariableValues(this.environment);
        String tenant = envInjector.injectEnvironmentVariableValues(this.tenant);
        String channel = envInjector.injectEnvironmentVariableValues(this.channel);
        String defaultPackageVersion = envInjector.injectEnvironmentVariableValues(this.defaultPackageVersion);

        logStartHeader(log);

        checkState(StringUtils.isNotBlank(project), String.format(OctoConstants.Errors.INPUT_CANNOT_BE_BLANK_MESSAGE_FORMAT, "Project name"));

        final List<String> commands = new ArrayList<>();
        commands.add(OctoConstants.Commands.CREATE_RELEASE);

        if (StringUtils.isNotBlank(releaseVersion)) {
            commands.add("--version");
            commands.add(releaseVersion);
        }

        if (StringUtils.isNotBlank(channel)) {
            commands.add("--channel");
            commands.add(channel);
        }

        if (deployThisRelease && StringUtils.isNotBlank(environment)) {
            final Iterable<String> environmentNameSplit = Splitter.on(',')
                    .trimResults()
                    .omitEmptyStrings()
                    .split(environment);
            for(final String env : environmentNameSplit) {
                commands.add("--deployTo");
                commands.add(env);
            }

            if (waitForDeployment) {
                commands.add("--progress");
            }

            if (StringUtils.isNotBlank(tenant)) {
                final Iterable<String> tenantSplit = Splitter.on(',')
                        .trimResults()
                        .omitEmptyStrings()
                        .split(tenant);
                for(final String t : tenantSplit) {
                    commands.add("--tenant");
                    commands.add(t);
                }
            }

            if (StringUtils.isNotBlank(tenantTag)) {
                final Iterable<String> tenantTagsSplit = Splitter.on(',')
                        .trimResults()
                        .omitEmptyStrings()
                        .split(tenantTag);
                for(final String tag : tenantTagsSplit) {
                    commands.add("--tenanttag");
                    commands.add(tag);
                }
            }
        }

        // Check packageVersion
        String releaseNotesContent = "";

        // Prepend Release Notes with Jenkins URL?
        // Do this regardless if Release Notes are specified
        if (releaseNotesJenkinsLinkback) {
            final String buildUrlVar = "${BUILD_URL}";
            final String jobNameVar = "${JOB_NAME}";
            final String buildNumberVar = "${BUILD_NUMBER}";

            // Use env vars
            String resolvedBuildUrlVar = envInjector.injectEnvironmentVariableValues(buildUrlVar);
            String resolvedJobNameVar = envInjector.injectEnvironmentVariableValues(jobNameVar);
            String resolvedBuildNumberVar = envInjector.injectEnvironmentVariableValues(buildNumberVar);

            releaseNotesContent = String.format("Release created by Build [%s #%s](%s)\n",
                resolvedJobNameVar,
                resolvedBuildNumberVar,
                resolvedBuildUrlVar);
        }

        if (releaseNotes) {
            if (isReleaseNotesSourceFile()) {
                try {
                    releaseNotesContent += getReleaseNotesFromFile(workspace, releaseNotesFile, log);
                } catch (Exception ex) {
                    log.fatal(String.format("Unable to get file contents from release notes file! - %s", ex.getMessage()));
                    success = false;
                }
            } else if (isReleaseNotesSourceScm()) {
                releaseNotesContent += getReleaseNotesFromScm(run);
            } else {
                log.fatal(String.format("Bad configuration: if using release notes, should have source of file or scm. Found '%s'", releaseNotesSource));
                success = false;
            }
        }

        if (!success) { // Early exit
            run.setResult(Result.FAILURE);
            return;
        }

        if (StringUtils.isNotBlank(releaseNotesContent)) {
            commands.add("--releaseNotes");
            commands.add(JSONSanitizer.getInstance().sanitize(releaseNotesContent));
        }

        if (StringUtils.isNotBlank(defaultPackageVersion)) {
            commands.add("--defaultPackageVersion");
            commands.add(defaultPackageVersion);
        }

        if (packageConfigs != null && !packageConfigs.isEmpty()) {
            for (final PackageConfiguration pkg : packageConfigs) {
                commands.add("--package");
                if (StringUtils.isNotBlank(pkg.getPackageReferenceName())) {
                    commands.add(String.format("%s:%s:%s", pkg.getPackageName(), pkg.getPackageReferenceName(), pkg.getPackageVersion()));
                } else {
                    commands.add(String.format("%s:%s", pkg.getPackageName(), pkg.getPackageVersion()));
                }
            }
        }

        if (this.deployThisRelease) {
            commands.addAll(getVariableCommands(run, envInjector, log, variables));
            if (run.getResult() == Result.FAILURE) {
                return;
            }
        }

        commands.addAll(getCommonCommandArguments(envInjector));

        try {
            final Boolean[] masks = getMasks(commands, OctoConstants.Commands.Arguments.MaskedArguments);
            Result result = launchOcto(workspace, launcher, commands, masks, envVars, listenerAdapter);
            success = result.equals(Result.SUCCESS);
            if (success) {
                String serverUrl = getOctopusDeployServer(serverId).getUrl();
                if (serverUrl.endsWith("/")) {
                    serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
                }
                OctopusApi api = getOctopusDeployServer(serverId).getApi().forSpace(spaceId);
                Project fullProject = api.getProjectsApi().getProjectByName(project, true);
                /*
                    It is not necessary to supply the release version, as this can (and probably will be in most cases)
                    generated by Octopus. If the version is not supplied, we link to the latest release for a project,
                    otherwise we link to the specified release.
                 */
                String urlSuffix = StringUtils.isBlank(releaseVersion)
                        ? api.getReleasesApi().getPortalUrlForLatestRelease(fullProject.getId())
                        : api.getReleasesApi().getPortalUrlForRelease(fullProject.getId(), releaseVersion);
                String portalUrl = serverUrl + urlSuffix;
                log.info("Release created: \n\t" + portalUrl);
                run.addAction(new BuildInfoSummary(BuildInfoSummary.OctopusDeployEventType.Release, portalUrl));

                if(deployThisRelease)
                {
                    Environment fullEnvironment = api.getEnvironmentsApi().getEnvironmentByName(environment, true);

                    String tenantId = null;
                    if (tenant != null && !tenant.isEmpty()) {
                        Tenant fullTenant = api.getTenantsApi().getTenantByName(tenant, true);
                        tenantId = fullTenant.getId();
                    }

                    String deploymenturlSuffix = api.getDeploymentsApi().getPortalUrlForDeployment(fullProject.getId(), releaseVersion, fullEnvironment.getId(), tenantId);

                    if (deploymenturlSuffix != null && !deploymenturlSuffix.isEmpty()) {
                        String portalDeploymentUrl = serverUrl + deploymenturlSuffix;
                        log.info("Deployment executed: \n\t" + portalDeploymentUrl);
                        run.addAction(new BuildInfoSummary(BuildInfoSummary.OctopusDeployEventType.Deployment, portalDeploymentUrl));
                    }
                }
            }
        } catch (Exception ex) {
            log.fatal("Failed to create release: " + ex.getMessage());
            success = false;
        }

        if (!success) {
            run.setResult(Result.FAILURE);
        }
    }

    /**
     * Write the startup header for the logs to show what our inputs are.
     * @param log The logger
     */
    private void logStartHeader(Log log) {
        log.info("Started Octopus Release");
        log.info("=======================");
        log.info("Project: " + project);
        log.info("Release Version: " + releaseVersion);
        if (channel != null && !channel.isEmpty()) {
            log.info("Channel: " + channel);
        }
        log.info("Include Release Notes?: " + releaseNotes);
        if (releaseNotes) {
            log.info("\tRelease Notes Source: " + releaseNotesSource);
            log.info("\tRelease Notes File: " + releaseNotesFile);
        }
        log.info("Deploy this Release?: " + deployThisRelease);
        if (deployThisRelease) {
            log.info("\tEnvironment: " + environment);
            log.info("\tWait for Deployment: " + waitForDeployment);
        }
        if (packageConfigs == null || packageConfigs.isEmpty()) {
            log.info("Package Configurations: none");
        } else {
            log.info("Package Configurations:");
            for (PackageConfiguration pc : packageConfigs) {
                log.info("\t" + pc.getPackageName() + "\t" + pc.getPackageReferenceName() + "\tv" + pc.getPackageVersion());
            }
        }
        log.info("=======================");
    }

    /**
     * Gets a package list that is a combination of the default packages (taken from the Octopus template)
     * and the packages selected. Selected package version overwrite the default package version for a given package
     * @param projectId
     * @param selectedPackages
     * @param defaultPackageVersion
     * @return A set that combines the default packages and selected packages
     */
    private Set<SelectedPackage> getCombinedPackageList(String projectId, List<PackageConfiguration> selectedPackages,
            String defaultPackageVersion, Log log, EnvironmentVariableValueInjector envInjector)
    {
        Set<SelectedPackage> combinedList = new HashSet<>();

        //Get all selected package names for easier lookup later
        Map<String, SelectedPackage> selectedNames = new HashMap<>();
        if (selectedPackages != null) {
            for (PackageConfiguration pkgConfig : selectedPackages) {
                SelectedPackage sp = new SelectedPackage(envInjector.injectEnvironmentVariableValues(pkgConfig.getPackageName()), null, pkgConfig.getPackageReferenceName(), envInjector.injectEnvironmentVariableValues(pkgConfig.getPackageVersion()));
                selectedNames.put(envInjector.injectEnvironmentVariableValues(pkgConfig.getPackageName()), sp);
                combinedList.add(sp);
            }
        }

        DeploymentProcessTemplate defaultPackages = null;
        //If not default version specified, ignore` all default packages
        try {
            defaultPackages = getApi().getDeploymentsApi().getDeploymentProcessTemplateForProject(projectId);
        } catch (Exception ex) {
            //Default package retrieval unsuccessful
            log.info(String.format("Could not retrieve default package list for project id: %s. No default packages will be used", projectId));
        }

        if (defaultPackages != null) {
            for (SelectedPackage selPkg : defaultPackages.getSteps()) {
                String stepName = selPkg.getStepName();
                String packageId = selPkg.getPackageId();
                String packageReferenceName = selPkg.getPackageReferenceName();

                //Only add if it was not a selected package
                if (!selectedNames.containsKey(stepName)) {
                    //If packageId specified replace by stepName retrieved from DeploymentProcessTemplate
                    //Emulates same behaviour as octo.client project https://octopus.com/docs/api-and-integration/octo.exe-command-line/creating-releases
                    if (selectedNames.containsKey(packageId)) {
                        SelectedPackage sc = selectedNames.get(packageId);
                        sc.setStepName(stepName);
                    } else {
                        //Get the default version, if not specified, warn
                        if (defaultPackageVersion != null && !defaultPackageVersion.isEmpty()) {
                            combinedList.add(new SelectedPackage(stepName, null, packageReferenceName, defaultPackageVersion));
                            log.info(String.format("Using default version (%s) of package %s", defaultPackageVersion, stepName));
                        } else {
                            log.error(String.format("Required package %s not included because package is not in Package Configuration list and no default package version defined", stepName));
                        }
                    }
                }
            }
        }

        return combinedList;
    }

    /**
     * Return the release notes contents from a file.
     * @param workspace our build
     * @return string contents of file
     * @throws IOException if there was a file read io problem
     * @throws InterruptedException if the action for reading was interrupted
     */
    private String getReleaseNotesFromFile(FilePath workspace, String releaseNotesFilename, Log log) throws IOException, InterruptedException {
        FilePath path = new FilePath(workspace, releaseNotesFilename);
        return path.act(new ReadFileCallable(log));
    }

    /**
     * This callable allows us to read files from other nodes - ie. Jenkins slaves.
     */
    private static final class ReadFileCallable implements FileCallable<String> {
        public final static String ERROR_READING = "<Error Reading File>";

        private final Log log;

        public ReadFileCallable(Log log)
        {
            this.log = log;
        }

        @Override
        public String invoke(File f, VirtualChannel channel) {
            try {
                return StringUtils.join(Files.readAllLines(f.toPath(), StandardCharsets.UTF_8), "\n");
            } catch (IOException ex) {
                log.error("Failed to read file: " + ex.getMessage());
                return ERROR_READING;
            }
        }

        @Override
        public void checkRoles(RoleChecker rc) throws SecurityException {

        }
    }

    /**
     * Attempt to load release notes info from SCM.
     * @param build the jenkins build
     * @return release notes as a single string
     */
    private String getReleaseNotesFromScm(Run<?, ?> build) {
        StringBuilder notes = new StringBuilder();

        Job project = build.getParent();
        Run lastSuccessfulBuild = project.getLastSuccessfulBuild();
        Run currentBuild = null;
        if (lastSuccessfulBuild == null) {
            Run lastBuild = project.getLastBuild();
            currentBuild = lastBuild;
        }
        else
        {
            currentBuild = lastSuccessfulBuild.getNextBuild();
        }
        if (currentBuild != null) {
            while (currentBuild != build)
            {
                String currBuildNotes = convertChangeSetToString(currentBuild);
                if (!currBuildNotes.isEmpty()) {
                    notes.append(currBuildNotes);
                }

                currentBuild = currentBuild.getNextBuild();
            }
            // Also include the current build
            String currBuildNotes = convertChangeSetToString(build);
            if (!currBuildNotes.isEmpty()) {
                notes.append(currBuildNotes);
            }
        }

        return notes.toString();
    }

    /**
     * Convert a build's change set to a string, each entry on a new line
     * @param run The build to poll changesets from
     * @return The changeset as a string
     */
    private String convertChangeSetToString(Run<?, ?> run) {
        StringBuilder allChangeNotes = new StringBuilder();
        if (run != null) {
            List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets = getChangeSets(run);

            for (ChangeLogSet<? extends ChangeLogSet.Entry> changeSet : changeSets)
                for (Object item : changeSet.getItems()) {
                    ChangeLogSet.Entry entry = (ChangeLogSet.Entry) item;
                    allChangeNotes.append(entry.getMsg()).append("\n");
                }

        }
        return allChangeNotes.toString();
    }

    @NotNull
    private List<ChangeLogSet<? extends ChangeLogSet.Entry>> getChangeSets(Run<?, ?> run) {
        if (run instanceof AbstractBuild) {
            AbstractBuild build = (AbstractBuild) run;
            return Collections.singletonList(build.getChangeSet());
        }
        else {
            WorkflowRun workflowRun = (WorkflowRun) run;
            return workflowRun.getChangeSets();
        }
    }

    /**
     * Descriptor for {@link OctopusDeployReleaseRecorder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    @Symbol("octopusCreateRelease")
    public static final class DescriptorImpl extends AbstractOctopusDeployDescriptorImplPost {
        private static final String PROJECT_RELEASE_VALIDATION_MESSAGE = "Project must be set to validate release.";
        private static final String SERVER_ID_VALIDATION_MESSAGE = "Could not validate without a valid Server ID.";

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Octopus Deploy: Create Release";
        }

        /**
         * Check that the project field is not empty and represents an actual project.
         * @param project The name of the project.
         * @param serverId The id of OctopusDeployServer in the configuration.
         * @param spaceId The id of the space where to load this resource from
         * @return FormValidation message if not ok.
         */
        public FormValidation doCheckProject(@QueryParameter String project, @QueryParameter String serverId, @QueryParameter String spaceId) {
            project = project.trim();

            serverId = serverId.trim();
            if (doCheckServerId(serverId).kind != FormValidation.Kind.OK) {
                return FormValidation.warning(SERVER_ID_VALIDATION_MESSAGE);
            }

            OctopusApi api = getApiByServerId(serverId).forSpace(spaceId);
            OctopusValidator validator = new OctopusValidator(api);
            return validator.validateProject(project);
        }

        /**
         * Check that the Channel field is either not set (default) or set to a real channel.
         * @param channel release channel.
         * @param project  The name of the project.
         * @param serverId The id of OctopusDeployServer in the configuration.
         * @param spaceId The id of the space where to load this resource from
         * @return Ok if not empty, error otherwise.
         */
        public FormValidation doCheckChannel(@QueryParameter String channel, @QueryParameter String project, @QueryParameter String serverId, @QueryParameter String spaceId) {
            channel = channel.trim();
            project = project.trim();

            serverId = serverId.trim();
            if (doCheckServerId(serverId).kind != FormValidation.Kind.OK) {
                return FormValidation.warning(SERVER_ID_VALIDATION_MESSAGE);
            }

            OctopusApi api = getApiByServerId(serverId).forSpace(spaceId);
            OctopusValidator validator = new OctopusValidator(api);
            return validator.validateChannel(channel, project);
        }

        /**
         * Check that the releaseVersion field is not empty.
         * @param releaseVersion release version.
         * @param project  The name of the project.
         * @param serverId The id of OctopusDeployServer in the configuration.
         * @param spaceId The id of the space where to load this resource from
         * @return Ok if not empty, error otherwise.
         */
        public FormValidation doCheckReleaseVersion(@QueryParameter String releaseVersion, @QueryParameter String project, @QueryParameter String serverId, @QueryParameter String spaceId) {
            releaseVersion = releaseVersion.trim();

            if (doCheckServerId(serverId).kind != FormValidation.Kind.OK) {
                return FormValidation.warning(SERVER_ID_VALIDATION_MESSAGE);
            }

            OctopusApi api = getApiByServerId(serverId).forSpace(spaceId);
            if (project == null || project.isEmpty()) {
                return FormValidation.warning(PROJECT_RELEASE_VALIDATION_MESSAGE);
            }
            com.octopusdeploy.api.data.Project p;
            try {
                p = api.getProjectsApi().getProjectByName(project);
                if (p == null) {
                    return FormValidation.warning("Unable to validate release because the project '%s' couldn't be found.", project);
                }
            } catch (Exception ex) {
                return FormValidation.warning(PROJECT_RELEASE_VALIDATION_MESSAGE);
            }

            OctopusValidator validator = new OctopusValidator(api);
            return validator.validateRelease(releaseVersion, p, OctopusValidator.ReleaseExistenceRequirement.MustNotExist);
        }

        /**
         * Check that the releaseNotesFile field is not empty.
         * @param releaseNotesFile The path to the release notes file, relative to the WS.
         * @return Ok if not empty, error otherwise.
         */
        public FormValidation doCheckReleaseNotesFile(@QueryParameter String releaseNotesFile) {
            if (releaseNotesFile.isEmpty()) {
                return FormValidation.error("Please provide a project notes file.");
            }

            return FormValidation.ok();
        }

        /**
         * Check that the environment field is not empty, and represents a real environment.
         * @param environment The name of the environment.
         * @param serverId The id of OctopusDeployServer in the configuration.
         * @param spaceId The id of the space where to load this resource from
         * @return FormValidation message if not ok.
         */
        public FormValidation doCheckEnvironment(@QueryParameter String environment, @QueryParameter String serverId, @QueryParameter String spaceId) {
            environment = environment.trim();

            if (doCheckServerId(serverId).kind != FormValidation.Kind.OK) {
                return FormValidation.warning(SERVER_ID_VALIDATION_MESSAGE);
            }

            OctopusApi api = getApiByServerId(serverId).forSpace(spaceId);
            OctopusValidator validator = new OctopusValidator(api);
            return validator.validateEnvironment(environment);
        }

        /**
         * Data binding that returns all possible environment names to be used in the environment autocomplete.
         * @param serverId The id of OctopusDeployServer in the configuration.
         * @param spaceId The id of the space where to load this resource from
         * @return ListBoxModel
         */
        public ComboBoxModel doFillEnvironmentItems(@QueryParameter String serverId, @QueryParameter String spaceId) {
            ComboBoxModel names = new ComboBoxModel();

            if (doCheckServerId(serverId).kind != FormValidation.Kind.OK) {
                return names;
            }

            OctopusApi api = getApiByServerId(serverId).forSpace(spaceId);
            try {
                Set<com.octopusdeploy.api.data.Environment> environments = api.getEnvironmentsApi().getAllEnvironments();
                for (com.octopusdeploy.api.data.Environment env : environments) {
                    names.add(env.getName());
                }
            } catch (Exception ex) {
                Logger.getLogger(OctopusDeployReleaseRecorder.class.getName()).log(Level.SEVERE, "Filling environments combo failed!", ex);
            }
            return names;
        }


        /**
         * Data binding that returns all possible tenant names to be used in the tenant autocomplete.
         * @param serverId The id of OctopusDeployServer in the configuration.
         * @param spaceId The id of the space where to load this resource from
         * @return ListBoxModel
         */
        public ListBoxModel doFillTenantItems(@QueryParameter String serverId, @QueryParameter String spaceId) {
            ListBoxModel names = new ListBoxModel();

            if (doCheckServerId(serverId).kind != FormValidation.Kind.OK) {
                return names;
            }

            OctopusApi api = getApiByServerId(serverId).forSpace(spaceId);
            try {
                Set<com.octopusdeploy.api.data.Tenant> tenants = api.getTenantsApi().getAllTenants();
                for (com.octopusdeploy.api.data.Tenant ten : tenants) {
                    names.add(ten.getName());
                }
            } catch (Exception ex) {
                Logger.getLogger(OctopusDeployDeploymentRecorder.class.getName()).log(Level.SEVERE, null, ex);
            }
            return names;
        }

        /**
         * Data binding that returns all possible tenant names to be used in the tenant autocomplete.
         * @param serverId The id of OctopusDeployServer in the configuration.
         * @param spaceId The id of the space where to load this resource from
         * @return ComboBoxModel
         */
        public ComboBoxModel doFillTenantTagItems(@QueryParameter String serverId, @QueryParameter String spaceId) {
            ComboBoxModel names = new ComboBoxModel();

            if (doCheckServerId(serverId).kind != FormValidation.Kind.OK) {
                return names;
            }

            OctopusApi api = getApiByServerId(serverId).forSpace(spaceId);
            try {
                Set<TagSet> tagSets = api.getTagSetsApi().getAll();
                for (TagSet tagSet : tagSets) {
                    for (Tag tag : tagSet.getTags()) {
                        names.add(tag.getCanonicalName());
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(OctopusDeployReleaseRecorder.class.getName()).log(Level.SEVERE, null, ex);
            }

            return names;
        }
        
        /**
         * Data binding that returns all possible project names to be used in the project autocomplete.
         * @param serverId The id of OctopusDeployServer in the configuration.
         * @param spaceId The id of the space where to load this resource from
         * @return ComboBoxModel
         */
        public ComboBoxModel doFillProjectItems(@QueryParameter String serverId, @QueryParameter String spaceId) {
            ComboBoxModel names = new ComboBoxModel();

            if (doCheckServerId(serverId).kind != FormValidation.Kind.OK) {
                return names;
            }

            OctopusApi api = getApiByServerId(serverId).forSpace(spaceId);
            try {
                Set<com.octopusdeploy.api.data.Project> projects = api.getProjectsApi().getAllProjects();
                for (com.octopusdeploy.api.data.Project proj : projects) {
                    names.add(proj.getName());
                }
            } catch (Exception ex) {
                Logger.getLogger(OctopusDeployReleaseRecorder.class.getName()).log(Level.SEVERE, "Filling projects combo failed!", ex);
            }
            return names;
        }
        
        /**
         * Data binding that returns all possible channels names to be used in the channel autocomplete.
         * @param project the project name
         * @param serverId The id of OctopusDeployServer in the configuration.
         * @param spaceId The id of the space where to load this resource from
         * @return ComboBoxModel
         */
        public ComboBoxModel doFillChannelItems(@QueryParameter String project, @QueryParameter String serverId, @QueryParameter String spaceId) {
            ComboBoxModel names = new ComboBoxModel();

            if (doCheckServerId(serverId).kind != FormValidation.Kind.OK) {
                return names;
            }

            OctopusApi api = getApiByServerId(serverId).forSpace(spaceId);
            if (project != null && !project.isEmpty()) {
                try {
                    com.octopusdeploy.api.data.Project p = api.getProjectsApi().getProjectByName(project);
                    if (p != null) {
                        Set<com.octopusdeploy.api.data.Channel> channels = api.getChannelsApi().getChannelsByProjectId(p.getId());
                        for (com.octopusdeploy.api.data.Channel channel : channels) {
                            names.add(channel.getName());
                        }
                    }
                } catch (Exception ex) {
                    Logger.getLogger(OctopusDeployReleaseRecorder.class.getName()).log(Level.SEVERE, "Filling Channel combo failed!", ex);
                }
            }
            return names;
        }
    }
}
