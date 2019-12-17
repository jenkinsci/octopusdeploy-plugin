package hudson.plugins.octopusdeploy;

import com.google.common.base.Splitter;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.plugins.octopusdeploy.constants.OctoConstants;
import hudson.plugins.octopusdeploy.services.OctopusBuildInformationBuilder;
import hudson.plugins.octopusdeploy.services.OctopusBuildInformationWriter;
import hudson.scm.ChangeLogSet;
import hudson.util.ListBoxModel;
import hudson.util.VariableResolver;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.types.Commandline;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;

public class OctopusDeployPushBuildInformationRecorder extends AbstractOctopusDeployRecorderBuildStep implements Serializable {

    private transient Log log;

    private final String packageId;
    public String getPackageId() { return packageId; }

    private final String packageVersion;
    public String getPackageVersion() { return packageVersion; }

    private final String commentParser;
    public String getCommentParser() { return commentParser; }

    private final OverwriteMode overwriteMode;
    public OverwriteMode getOverwriteMode() { return overwriteMode; }

    @DataBoundConstructor
    public OctopusDeployPushBuildInformationRecorder(String serverId, String spaceId, String toolId, String packageId,
                                                     String packageVersion, String commentParser, OverwriteMode overwriteMode,
                                                     Boolean verboseLogging, String additionalArgs) {
        this.serverId = serverId.trim();
        this.spaceId = spaceId.trim();
        this.toolId = toolId.trim();
        this.packageId = packageId.trim();
        this.packageVersion = packageVersion.trim();
        this.commentParser = commentParser.trim();
        this.overwriteMode = overwriteMode;
        this.verboseLogging = verboseLogging;
        this.additionalArgs = additionalArgs.trim();
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        boolean success = true;
        log = new Log(listener);
        if (Result.FAILURE.equals(build.getResult())) {
            log.info("Not pushing build information due to job being in FAILED state.");
            return success;
        }

        VariableResolver resolver = build.getBuildVariableResolver();
        EnvVars envVars;
        try {
            envVars = build.getEnvironment(listener);
        } catch (Exception ex) {
            log.fatal(String.format("Failed to retrieve environment variables for this build '%s' - '%s'",
                    build.getProject().getName(), ex.getMessage()));
            return false;
        }
        EnvironmentVariableValueInjector envInjector = new EnvironmentVariableValueInjector(resolver, envVars);

        //logStartHeader


        try {
            final List<String> commands = buildCommands(build, envInjector);
            final Boolean[] masks = getMasks(commands, OctoConstants.Commands.Arguments.MaskedArguments);
            Result result = launchOcto(launcher, commands, masks, envVars, listener);
            success = result.equals(Result.SUCCESS);
        } catch (Exception ex) {
            log.fatal("Failed to push the build information: " + ex.getMessage());
            success = false;
        }

        return success;
    }

    private List<String> buildCommands(final AbstractBuild build, final EnvironmentVariableValueInjector envInjector) throws IOException, InterruptedException {
        final List<String> commands = new ArrayList<>();

        OctopusDeployServer server = getOctopusDeployServer(this.serverId);
        String serverUrl = server.getUrl();
        String apiKey = server.getApiKey().getPlainText();
        boolean ignoreSslErrors = server.getIgnoreSslErrors();
        OverwriteMode overwriteMode = this.overwriteMode;
        String packageIds = envInjector.injectEnvironmentVariableValues(this.packageId);
        String additionalArgs = envInjector.injectEnvironmentVariableValues(this.additionalArgs);

        checkState(StringUtils.isNotBlank(serverUrl), String.format(OctoConstants.Errors.INPUT_CANNOT_BE_BLANK_MESSAGE_FORMAT, "Octopus URL"));
        checkState(StringUtils.isNotBlank(apiKey), String.format(OctoConstants.Errors.INPUT_CANNOT_BE_BLANK_MESSAGE_FORMAT, "API Key"));

        commands.add("build-information");

        commands.add("--server");
        commands.add(serverUrl);

        commands.add("--apiKey");
        commands.add(apiKey);

        if (StringUtils.isNotBlank(spaceId)) {
            commands.add("--space");
            commands.add(spaceId);
        }

        if (StringUtils.isNotBlank(packageIds)) {
            final Iterable<String> packageIdsSplit = Splitter.on("\n")
                    .trimResults()
                    .omitEmptyStrings()
                    .split(packageIds);
            for(final String packageId : packageIdsSplit) {
                commands.add("--package-id");
                commands.add(packageId);
            }
        }

        commands.add("--version");
        commands.add(packageVersion);

        final String buildInformationFile = getBuildInformationFromScm(build, envInjector);
        commands.add("--file");
        commands.add(buildInformationFile);

        if (overwriteMode != OverwriteMode.FailIfExists) {
            commands.add("--overwrite-mode");
            commands.add(overwriteMode.name());
        }

        if (verboseLogging) {
            commands.add("--debug");
        }

        if (ignoreSslErrors) {
            commands.add("--ignoreSslErrors");
        }

        if(StringUtils.isNotBlank(additionalArgs)) {
            final String[] myArgs = Commandline.translateCommandline(additionalArgs);
            commands.addAll(Arrays.asList(myArgs));
        }

        return commands;
    }
    /**
     * Attempt to load release notes info from SCM.
     * @param build the jenkins build
     * @param envInjector the environment variable injector
     * @return path to build information file
     */
    private String getBuildInformationFromScm(AbstractBuild build, EnvironmentVariableValueInjector envInjector) throws IOException, InterruptedException {
        String checkoutDir = build.getWorkspace().getRemote();
        final String buildInformationFile = Paths.get(checkoutDir, "octopus.buildinfo").toAbsolutePath().toString();
        AbstractProject project = build.getProject();

        final OctopusBuildInformationBuilder builder = new OctopusBuildInformationBuilder();
        final OctopusBuildInformation buildInformation = builder.build(
                getVcsType(project),
                envInjector.injectEnvironmentVariableValues("${GIT_URL}"),
                envInjector.injectEnvironmentVariableValues("${GIT_COMMIT}"),
                getCommits(build, project),
                commentParser,
                envInjector.injectEnvironmentVariableValues("${BUILD_URL}"),
                Integer.toString(build.getNumber())
        );

        if (verboseLogging) {
            log.info("Creating " + buildInformationFile);
        }
        final OctopusBuildInformationWriter writer = new OctopusBuildInformationWriter(log, verboseLogging);
        writer.writeToFile(buildInformation, buildInformationFile);

        return buildInformationFile;
    }

    private String getVcsType(AbstractProject project) {
        final String scmType = project.getScm().getType().toLowerCase();
        if (scmType.contains("git")) {
            return "Git";
        } else if (scmType.contains("cvs")) {
            return "CVS";
        }
        return "Unknown";
    }

    private List<Commit> getCommits(AbstractBuild build, AbstractProject project) {
        List<Commit> commits = new ArrayList<>();
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
                commits.addAll(convertChangeSetToCommits(currentBuild));

                currentBuild = currentBuild.getNextBuild();
            }
            // Also include the current build
            commits.addAll(convertChangeSetToCommits(build));
        }
        return commits;
    }

    /**
     * Convert a build's change set to a string, each entry on a new line
     * @param build The build to poll changesets from
     * @return The changeset as a string
     */
    private List<Commit> convertChangeSetToCommits(AbstractBuild build) {
        List<Commit> commits = new ArrayList<>();
        if (build != null) {
            ChangeLogSet<? extends ChangeLogSet.Entry> changeSet = build.getChangeSet();
            for (Object item : changeSet.getItems()) {
                ChangeLogSet.Entry entry = (ChangeLogSet.Entry) item;
                final Commit commit = new Commit();
                commit.Id = entry.getCommitId();
                commit.Comment = entry.getMsg();
                commits.add(commit);
            }
        }
        return commits;
    }

    @Extension
    public static final class DescriptorImpl extends AbstractOctopusDeployDescriptorImplStep {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Octopus Deploy: Push build information";
        }

        public ListBoxModel doFillCommentParserItems() {
            final ListBoxModel items = new ListBoxModel();
            items.add("", "");
            items.add(CommentParser.Jira.name(), CommentParser.Jira.name());
            items.add(CommentParser.GitHub.name(), CommentParser.GitHub.name());
            return items;
        }
    }
}
