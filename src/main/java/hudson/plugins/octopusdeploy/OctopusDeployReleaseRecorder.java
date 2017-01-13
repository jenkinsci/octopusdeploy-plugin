package hudson.plugins.octopusdeploy;

import com.octopusdeploy.api.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import jenkins.model.*;
import hudson.*;
import hudson.FilePath.FileCallable;
import hudson.model.*;
import hudson.remoting.VirtualChannel;
import hudson.scm.*;
import hudson.tasks.*;
import hudson.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.*;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.*;
import org.kohsuke.stapler.export.*;

/**
 * Creates a release and optionally deploys it.
 */
public class OctopusDeployReleaseRecorder extends Recorder implements Serializable {
    /**
     * The project name as defined in Octopus.
     */
    private final String project;
    public String getProject() {
        return project;
    }

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

    /**
     * The Tenant to use for a deploy to in Octopus.
     */
    private final String tenant;
    public String getTenant() {
        return tenant;
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
     * If we are deploying, should we wait for it to complete?
     */
    private final boolean waitForDeployment;
    public boolean getWaitForDeployment() {
        return waitForDeployment;
    }

    /**
     * The environment to deploy to, if we are deploying.
     */
    private final String environment;
    public String getEnvironment() {
        return environment;
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
            String project, String releaseVersion,
            boolean releaseNotes, String releaseNotesSource, String releaseNotesFile,
            boolean deployThisRelease, String environment, String tenant, boolean waitForDeployment,
            List<PackageConfiguration> packageConfigs, boolean jenkinsUrlLinkback,
            String defaultPackageVersion) {
        this.project = project.trim();
        this.releaseVersion = releaseVersion.trim();
        this.releaseNotes = releaseNotes;
        this.releaseNotesSource = releaseNotesSource;
        this.releaseNotesFile = releaseNotesFile.trim();
        this.deployThisRelease = deployThisRelease;
        this.packageConfigs = packageConfigs;
        this.environment = environment.trim();
        this.tenant = tenant.trim();
        this.waitForDeployment = waitForDeployment;
        this.releaseNotesJenkinsLinkback = jenkinsUrlLinkback;
        this.defaultPackageVersion = defaultPackageVersion;
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
        logStartHeader(log);
        // todo: getting from descriptor is ugly. refactor?
        getDescriptorImpl().setGlobalConfiguration();
        OctopusApi api = getDescriptorImpl().api;
        VariableResolver resolver = build.getBuildVariableResolver();
        EnvVars envVars;
        try {
            envVars = build.getEnvironment(listener);
        } catch (Exception ex) {
            log.fatal(String.format("Failed to retrieve environment variables for this build - '%s'",
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
        String defaultPackageVersion = envInjector.injectEnvironmentVariableValues(this.defaultPackageVersion);

        com.octopusdeploy.api.Project p = null;
        try {
            p = api.getProjectByName(project);
        } catch (Exception ex) {
            log.fatal(String.format("Retrieving project name '%s' failed with message '%s'",
                    project, ex.getMessage()));
            success = false;
        }
        if (p == null) {
            log.fatal("Project was not found.");
            success = false;
        }

        // Check packageVersion
        String releaseNotesContent = "";

        // Prepend Release Notes with Jenkins URL?
        // Do this regardless if Release Notes are specified
        if (releaseNotesJenkinsLinkback) {
            final String buildUrlVar = "${BUILD_URL}";

            // Use env vars
            String resolvedBuildUrlVar = envInjector.injectEnvironmentVariableValues(buildUrlVar);
            releaseNotesContent = String.format("Created by: <a href=\"%s\">%s</a>\n",
                resolvedBuildUrlVar,
                resolvedBuildUrlVar);
        }

        if (releaseNotes) {
            if (isReleaseNotesSourceFile()) {
                try {
                    releaseNotesContent += getReleaseNotesFromFile(build, releaseNotesFile);
                } catch (Exception ex) {
                    log.fatal(String.format("Unable to get file contents from release ntoes file! - %s", ex.getMessage()));
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

        Set<SelectedPackage> selectedPackages = null;
        List<PackageConfiguration> combinedPackageConfigs = getCombinedPackageList(p.getId(), packageConfigs, defaultPackageVersion, log);
        if (combinedPackageConfigs != null && !combinedPackageConfigs.isEmpty()) {
            selectedPackages = new HashSet<SelectedPackage>();
            for (PackageConfiguration pc : combinedPackageConfigs) {
                selectedPackages.add(new SelectedPackage(
                    envInjector.injectEnvironmentVariableValues(pc.getPackageName()),
                    envInjector.injectEnvironmentVariableValues(pc.getPackageVersion())));
            }
        }

        try {
            // Sanitize the release notes in preparation for JSON
            releaseNotesContent = JSONSanitizer.getInstance().sanitize(releaseNotesContent);

            String results = api.createRelease(p.getId(), releaseVersion, releaseNotesContent, selectedPackages);
            JSONObject json = (JSONObject)JSONSerializer.toJSON(results);
            String urlSuffix = json.getJSONObject("Links").getString("Web");
            String url = getDescriptorImpl().octopusHost;
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 2);
            }
            log.info("Release created: \n\t" + url + urlSuffix);
            build.addAction(new BuildInfoSummary(BuildInfoSummary.OctopusDeployEventType.Release, url + urlSuffix));
        } catch (Exception ex) {
            log.fatal("Failed to create release: " + ex.getMessage());
            success = false;
        }

        if (success && deployThisRelease) {
          OctopusDeployDeploymentRecorder deployment = new OctopusDeployDeploymentRecorder(project, releaseVersion, environment, tenant, "", waitForDeployment);
          success = deployment.perform(build, launcher, listener);
        }

        return success;
    }

    private DescriptorImpl getDescriptorImpl() {
        return ((DescriptorImpl)getDescriptor());
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
                log.info("\t" + pc.getPackageName() + "\tv" + pc.getPackageVersion());
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
     * @return A list that combines the default packages and selected packages
     */
    private List<PackageConfiguration> getCombinedPackageList(String projectId, List<PackageConfiguration> selectedPackages,
            String defaultPackageVersion, Log log)
    {
        List<PackageConfiguration> combinedList = new ArrayList<PackageConfiguration>();

        //Get all selected package names for easier lookup later
        Set<String> selectedNames = new HashSet<String>();
        if (selectedPackages != null) {
            for (PackageConfiguration pkgConfig : selectedPackages) {
                selectedNames.add(pkgConfig.getPackageName());
            }
            //Start with the selected packages
            combinedList.addAll(selectedPackages);
        }

        DeploymentProcessTemplate defaultPackages = null;
        //If not default version specified, ignore all default packages
        try {
            defaultPackages = this.getDescriptorImpl().api.getDeploymentProcessTemplateForProject(projectId);
        } catch (Exception ex) {
            //Default package retrieval unsuccessful
            log.info(String.format("Could not retrieve default package list for project id: %s. No default packages will be used", projectId));
        }

        if (defaultPackages != null) {
            for (SelectedPackage selPkg : defaultPackages.getSteps()) {
                String name = selPkg.getStepName();

                //Only add if it was not a selected package
                if (!selectedNames.contains(name)) {
                    //Get the default version, if not specified, warn
                    if (defaultPackageVersion != null && !defaultPackageVersion.isEmpty()) {
                        combinedList.add(new PackageConfiguration(name, defaultPackageVersion));
                        log.info(String.format("Using default version (%s) of package %s", defaultPackageVersion, name));
                    }
                    else {
                        log.error(String.format("Required package %s not included because package is not in Package Configuration list and no default package version defined", name));
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
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        private String octopusHost;
        private String apiKey;
        private boolean loadedConfig;
        private OctopusApi api;
        private static final String PROJECT_RELEASE_VALIDATION_MESSAGE = "Project must be set to validate release.";

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "OctopusDeploy Release";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
            save();
            return true;
        }

        /**
        * Loads the OctopusDeployPlugin descriptor and pulls configuration from it
        * for API Key, and Host.
        */
        private void setGlobalConfiguration() {
            // NOTE  - This method is not being called from the constructor due
            // to a circular dependency issue on startup
            if (!loadedConfig) {
                updateGlobalConfiguration();
            }
        }

        public void updateGlobalConfiguration() {
            OctopusDeployPlugin.DescriptorImpl descriptor = (OctopusDeployPlugin.DescriptorImpl)
                    Jenkins.getInstance().getDescriptor(OctopusDeployPlugin.class);
             apiKey = descriptor.getApiKey();
             octopusHost = descriptor.getOctopusHost();
             api = new OctopusApi(octopusHost, apiKey);
             loadedConfig = true;
        }

        /**
         * Check that the project field is not empty and represents an actual project.
         * @param project The name of the project.
         * @return FormValidation message if not ok.
         */
        public FormValidation doCheckProject(@QueryParameter String project) {
            setGlobalConfiguration();
            project = project.trim();
            OctopusValidator validator = new OctopusValidator(api);
            return validator.validateProject(project);
        }

        /**
         * Check that the releaseVersion field is not empty.
         * @param releaseVersion release version.
         * @param project  The name of the project.
         * @return Ok if not empty, error otherwise.
         */
        public FormValidation doCheckReleaseVersion(@QueryParameter String releaseVersion, @QueryParameter String project) {
            setGlobalConfiguration();
            releaseVersion = releaseVersion.trim();
            if (project == null || project.isEmpty()) {
                return FormValidation.warning(PROJECT_RELEASE_VALIDATION_MESSAGE);
            }
            com.octopusdeploy.api.Project p;
            try {
                p = api.getProjectByName(project);
                if (p == null) {
                    return FormValidation.warning(PROJECT_RELEASE_VALIDATION_MESSAGE);
                }
            } catch (Exception ex) {
                return FormValidation.warning(PROJECT_RELEASE_VALIDATION_MESSAGE);
            }

            OctopusValidator validator = new OctopusValidator(api);
            return validator.validateRelease(releaseVersion, p.getId(), OctopusValidator.ReleaseExistenceRequirement.MustNotExist);
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
         * @return FormValidation message if not ok.
         */
        public FormValidation doCheckEnvironment(@QueryParameter String environment) {
            setGlobalConfiguration();
            environment = environment.trim();
            OctopusValidator validator = new OctopusValidator(api);
            return validator.validateEnvironment(environment);
        }

        /**
         * Data binding that returns all possible environment names to be used in the environment autocomplete.
         * @return ComboBoxModel
         */
        public ComboBoxModel doFillEnvironmentItems() {
            setGlobalConfiguration();
            ComboBoxModel names = new ComboBoxModel();

            try {
                Set<com.octopusdeploy.api.Environment> environments = api.getAllEnvironments();
                for (com.octopusdeploy.api.Environment env : environments) {
                    names.add(env.getName());
                }
            } catch (Exception ex) {
                Logger.getLogger(OctopusDeployReleaseRecorder.class.getName()).log(Level.SEVERE, "Filling environments combo failed!", ex);
            }
            return names;
        }

        /**
         * Data binding that returns all possible tenant names to be used in the tenant autocomplete.
         * @return ComboBoxModel
         */
        public ComboBoxModel doFillTenantItems() {
            setGlobalConfiguration();
            ComboBoxModel names = new ComboBoxModel();
            try {
                Set<com.octopusdeploy.api.Tenant> tenants = api.getAllTenants();
                for (com.octopusdeploy.api.Tenant ten : tenants) {
                    names.add(ten.getName());
                }
            } catch (Exception ex) {
                Logger.getLogger(OctopusDeployDeploymentRecorder.class.getName()).log(Level.SEVERE, null, ex);
            }
            return names;
        }
        
        /**
         * Data binding that returns all possible project names to be used in the project autocomplete.
         * @return ComboBoxModel
         */
        public ComboBoxModel doFillProjectItems() {
            setGlobalConfiguration();
            ComboBoxModel names = new ComboBoxModel();
            try {
                Set<com.octopusdeploy.api.Project> projects = api.getAllProjects();
                for (com.octopusdeploy.api.Project proj : projects) {
                    names.add(proj.getName());
                }
            } catch (Exception ex) {
                Logger.getLogger(OctopusDeployReleaseRecorder.class.getName()).log(Level.SEVERE, "Filling projects combo failed!", ex);
            }
            return names;
        }
    }
}
