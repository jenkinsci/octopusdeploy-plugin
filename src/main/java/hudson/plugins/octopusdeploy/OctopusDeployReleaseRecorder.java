package hudson.plugins.octopusdeploy;

import com.google.common.base.Splitter;
import com.octopusdeploy.api.data.SelectedPackage;
import com.octopusdeploy.api.data.DeploymentProcessTemplate;
import com.octopusdeploy.api.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import com.octopusdeploy.api.data.Tag;
import com.octopusdeploy.api.data.TagSet;
import hudson.*;
import hudson.FilePath.FileCallable;
import hudson.model.*;
import hudson.plugins.octopusdeploy.constants.OctoConstants;
import hudson.remoting.VirtualChannel;
import hudson.scm.*;
import hudson.tasks.*;
import hudson.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.*;
import org.kohsuke.stapler.export.*;

import static com.google.common.base.Preconditions.checkState;

/**
 * Creates a release and optionally deploys it.
 */
public class OctopusDeployReleaseRecorder extends AbstractOctopusDeployRecorder implements Serializable {
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
    private final boolean releaseNotes;
    public boolean getReleaseNotes() {
        return releaseNotes;
    }

    /**
     * Where are the release notes located?
     */
    private final String releaseNotesSource;
    public String getReleaseNotesSource() {
        return releaseNotesSource;
    }

    public boolean isReleaseNotesSourceFile() {
        return "file".equals(releaseNotesSource);
    }

    public boolean isReleaseNotesSourceScm() {
        return "scm".equals(releaseNotesSource);
    }
    
    private final String channel;
    public String getChannel() {
        return channel;
    }

    /**
     * Write a link back to the originating Jenkins build to the
     * Octopus release notes?
     */
    private final boolean releaseNotesJenkinsLinkback;
    public boolean getJenkinsUrlLinkback() {
        return releaseNotesJenkinsLinkback;
    }

    /**
     * The file that the release notes are in.
     */
    private final String releaseNotesFile;
    public String getReleaseNotesFile() {
        return releaseNotesFile;
    }

    /**
     * Should this release be deployed right after it is created?
     */
    private final boolean deployThisRelease;
    @Exported
    public boolean getDeployThisRelease() {
        return deployThisRelease;
    }

    /**
     * All packages needed to create this new release.
     */
    private final List<PackageConfiguration> packageConfigs;
    @Exported
    public List<PackageConfiguration> getPackageConfigs() {
        return packageConfigs;
    }

    /**
     * Default package version to use for required packages that are not
     * specified in the Package Configurations
     */
    private final String defaultPackageVersion;
    @Exported
    public String getDefaultPackageVersion() {
        return defaultPackageVersion;
    }

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public OctopusDeployReleaseRecorder(
            String serverId, String toolId, String spaceId, String project, String releaseVersion,
            boolean releaseNotes, String releaseNotesSource, String releaseNotesFile,
            boolean deployThisRelease, String environment, String tenant, String tenantTag, String channel, boolean waitForDeployment,
            String deploymentTimeout, boolean cancelOnTimeout,
            List<PackageConfiguration> packageConfigs, boolean jenkinsUrlLinkback,
            String defaultPackageVersion, boolean verboseLogging, String additionalArgs) {

        this.serverId = serverId.trim();
        this.toolId = toolId.trim();
        this.spaceId = spaceId.trim();
        this.project = project.trim();
        this.releaseVersion = releaseVersion.trim();
        this.releaseNotes = releaseNotes;
        this.releaseNotesSource = releaseNotesSource;
        this.releaseNotesFile = releaseNotesFile.trim();
        this.deployThisRelease = deployThisRelease;
        this.packageConfigs = packageConfigs;
        this.environment = environment.trim();
        this.tenant = tenant == null ? null : tenant.trim();
        this.tenantTag = tenantTag == null ? null : tenantTag.trim();
        this.channel = channel == null ? null : channel.trim();
        this.waitForDeployment = waitForDeployment;
        this.deploymentTimeout = deploymentTimeout == null ? null : deploymentTimeout.trim();
        this.cancelOnTimeout = cancelOnTimeout;
        this.releaseNotesJenkinsLinkback = jenkinsUrlLinkback;
        this.defaultPackageVersion = defaultPackageVersion;
        this.verboseLogging = verboseLogging;
        this.additionalArgs = additionalArgs == null ? null : additionalArgs.trim();
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        boolean success = true;
        Log log = new Log(listener);
        if (Result.FAILURE.equals(build.getResult())) {
            log.info("Not creating a release due to job being in FAILED state.");
            return success;
        }

        VariableResolver resolver = build.getBuildVariableResolver();
        EnvVars envVars;
        try {
            envVars = build.getEnvironment(listener);
        } catch (Exception ex) {
            log.fatal(String.format("Failed to retrieve environment variables for this project '%s' - '%s'",
                project, ex.getMessage()));
            return false;
        }
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

            releaseNotesContent = String.format("Release created by Build [%s #%s](%s)",
                resolvedJobNameVar,
                resolvedBuildNumberVar,
                resolvedBuildUrlVar);
        }

        if (releaseNotes) {
            if (isReleaseNotesSourceFile()) {
                try {
                    releaseNotesContent += getReleaseNotesFromFile(build, releaseNotesFile);
                } catch (Exception ex) {
                    log.fatal(String.format("Unable to get file contents from release notes file! - %s", ex.getMessage()));
                    success = false;
                }
            } else if (isReleaseNotesSourceScm()) {
                releaseNotesContent += getReleaseNotesFromScm(build);
            } else {
                log.fatal(String.format("Bad configuration: if using release notes, should have source of file or scm. Found '%s'", releaseNotesSource));
                success = false;
            }
        }

        if (!success) { // Early exit
            return success;
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

        commands.addAll(getCommonCommandArguments());

        try {
            final Boolean[] masks = getMasks(commands, OctoConstants.Commands.Arguments.MaskedArguments);
            Result result = launchOcto(launcher, commands, masks, envVars, listener);
            success = result.equals(Result.SUCCESS);
            if (success) {
                //build.addAction(new BuildInfoSummary(BuildInfoSummary.OctopusDeployEventType.Release, serverUrl + urlSuffix));
            }
        } catch (Exception ex) {
            log.fatal("Failed to create release: " + ex.getMessage());
            success = false;
        }

        return success;
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
     * @param build our build
     * @return string contents of file
     * @throws IOException if there was a file read io problem
     * @throws InterruptedException if the action for reading was interrupted
     */
    private String getReleaseNotesFromFile(AbstractBuild build, String releaseNotesFilename) throws IOException, InterruptedException {
        FilePath path = new FilePath(build.getWorkspace(), releaseNotesFilename);
        return path.act(new ReadFileCallable());
    }

    /**
     * This callable allows us to read files from other nodes - ie. Jenkins slaves.
     */
    private static final class ReadFileCallable implements FileCallable<String> {
        public final static String ERROR_READING = "<Error Reading File>";

        @Override
        public String invoke(File f, VirtualChannel channel) {
            try {
                return StringUtils.join(Files.readAllLines(f.toPath(), StandardCharsets.UTF_8), "\n");
            } catch (IOException ex) {
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
    private String getReleaseNotesFromScm(AbstractBuild build) {
        StringBuilder notes = new StringBuilder();
        AbstractProject project = build.getProject();
        AbstractBuild lastSuccessfulBuild = (AbstractBuild)project.getLastSuccessfulBuild();
        AbstractBuild currentBuild = null;
        if (lastSuccessfulBuild == null) {
            AbstractBuild lastBuild = (AbstractBuild)project.getLastBuild();
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
     * @param build The build to poll changesets from
     * @return The changeset as a string
     */
    private String convertChangeSetToString(AbstractBuild build) {
        StringBuilder allChangeNotes = new StringBuilder();
        if (build != null) {
            ChangeLogSet<? extends ChangeLogSet.Entry> changeSet = build.getChangeSet();
            for (Object item : changeSet.getItems()) {
                ChangeLogSet.Entry entry = (ChangeLogSet.Entry) item;
                allChangeNotes.append(entry.getMsg()).append("\n");
            }
        }
        return allChangeNotes.toString();
    }

    /**
     * Descriptor for {@link OctopusDeployReleaseRecorder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends AbstractOctopusDeployDescriptorImpl {
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
         * @param spaceId The id of the space the project is in
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
         * @param spaceId The id of the space the project is in
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
         * @param spaceId The id of the space the project is in
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
                    return FormValidation.warning(PROJECT_RELEASE_VALIDATION_MESSAGE);
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
         * @param spaceId The id of the space the project is in
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
         * @param spaceId The id of the space the project is in
         * @return ComboBoxModel
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
         * @param spaceId The id of the space the project is in
         * @return ComboBoxModel
         */
        public ComboBoxModel doFillTenantItems(@QueryParameter String serverId, @QueryParameter String spaceId) {
            ComboBoxModel names = new ComboBoxModel();

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
         * @param spaceId The id of the space the project is in
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
         * @param spaceId The id of the space the project is in
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
