package hudson.plugins.octopusdeploy;

import com.google.common.base.Splitter;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.octopusdeploy.constants.OctoConstants;
import hudson.plugins.octopusdeploy.services.OctopusBuildInformationBuilder;
import hudson.plugins.octopusdeploy.services.OctopusBuildInformationWriter;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCM;
import hudson.util.ListBoxModel;
import hudson.util.VariableResolver;
import jenkins.util.BuildListenerAdapter;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.types.Commandline;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;

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

    private String gitUrl;
    public String getGitUrl() {
        return this.gitUrl;
    }

    @DataBoundSetter
    public void setGitUrl(String gitUrl) {
        this.gitUrl = gitUrl == null ? null : gitUrl.trim();
    }

    private String gitCommit;
    public String getGitCommit() {
        return this.gitCommit;
    }

    @DataBoundSetter
    public void setGitCommit(String gitCommit) {
        this.gitCommit = gitCommit == null ? null : gitCommit.trim();
    }

    @DataBoundConstructor
    public OctopusDeployPushBuildInformationRecorder(String serverId, String spaceId, String toolId, String packageId,
                                                     String packageVersion, String commentParser, OverwriteMode overwriteMode) {
        this.serverId = serverId.trim();
        this.spaceId = spaceId.trim();
        this.toolId = toolId.trim();
        this.packageId = packageId.trim();
        this.packageVersion = packageVersion.trim();
        this.commentParser = commentParser.trim();
        this.overwriteMode = overwriteMode;
        this.verboseLogging = false;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) {
        boolean success = true;
        BuildListenerAdapter listenerAdapter = new BuildListenerAdapter(listener);

        log = new Log(listenerAdapter);
        if (Result.FAILURE.equals(run.getResult())) {
            log.info("Not pushing build information due to job being in FAILED state.");
            return;
        }

        EnvVars envVars;
        try {
            envVars = run.getEnvironment(listener);
        } catch (Exception ex) {
            log.fatal(String.format("Failed to retrieve environment variables for this build '%s' - '%s'",
                    run.getParent().getName(), ex.getMessage()));
            run.setResult(Result.FAILURE);
            return;
        }
        VariableResolver resolver =  new VariableResolver.ByMap<>(envVars);
        EnvironmentVariableValueInjector envInjector = new EnvironmentVariableValueInjector(resolver, envVars);

        //logStartHeader


        try {
            final List<String> commands = buildCommands(run, envInjector, workspace);
            final Boolean[] masks = getMasks(commands, OctoConstants.Commands.Arguments.MaskedArguments);
            Result result = launchOcto(workspace, launcher, commands, masks, envVars, listenerAdapter);
            success = result.equals(Result.SUCCESS);
        } catch (Exception ex) {
            log.fatal("Failed to push the build information: " + ex.getMessage());
            success = false;
        }

        if (!success) {
            run.setResult(Result.FAILURE);
        }
    }

    private List<String> buildCommands(final Run<?, ?> build, final EnvironmentVariableValueInjector envInjector, FilePath workspace) throws IOException, InterruptedException {
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

        final String buildInformationFile = getBuildInformationFromScm(build, envInjector, workspace);
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
     * @param workspace
     * @return path to build information file
     */
    private String getBuildInformationFromScm(Run<?, ?> build, EnvironmentVariableValueInjector envInjector, FilePath workspace) throws IOException, InterruptedException {
        FilePath ws = workspace;
        Job project = build.getParent();

        String gitUrl = isNullOrEmpty(this.getGitUrl()) ? envInjector.injectEnvironmentVariableValues("${GIT_URL}") : envInjector.injectEnvironmentVariableValues(this.getGitUrl());
        String gitCommit = isNullOrEmpty(this.getGitCommit())?  envInjector.injectEnvironmentVariableValues("${GIT_COMMIT}") : envInjector.injectEnvironmentVariableValues(this.getGitCommit());
        final OctopusBuildInformationBuilder builder = new OctopusBuildInformationBuilder();
        final OctopusBuildInformation buildInformation = builder.build(
                getVcsType(project),
                gitUrl,
                gitCommit,
                getCommits(build, project),
                commentParser,
                envInjector.injectEnvironmentVariableValues("${BUILD_URL}"),
                Integer.toString(build.getNumber())
        );

        final String buildInformationFile = "octopus.buildinfo";
        if (verboseLogging) {
            log.info("Creating " + buildInformationFile + " in " + ws.getRemote());
        }
        final OctopusBuildInformationWriter writer = new OctopusBuildInformationWriter(log, verboseLogging);
        writer.writeToFile(ws, buildInformation, buildInformationFile);

        return buildInformationFile;
    }

    private String getVcsType(Job job) {
        SCM scm;
        if (job instanceof AbstractProject) {
            AbstractProject project = (AbstractProject) job;
            scm = project.getScm();
        }
        else {
            WorkflowJob workflowJob = (WorkflowJob) job;
            scm = workflowJob.getTypicalSCM();
        }

        if (scm == null)
            return "Unknown";

        final String scmType = scm.getType().toLowerCase();
        if (scmType.contains("git")) {
            return "Git";
        } else if (scmType.contains("cvs")) {
            return "CVS";
        }
        return "Unknown";
    }

    private List<Commit> getCommits(Run<?,?> build, Job project) {
        List<Commit> commits = new ArrayList<>();
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
     * @param run The build to poll changesets from
     * @return The changeset as a string
     */
    private List<Commit> convertChangeSetToCommits(Run<?,?> run) {
        List<Commit> commits = new ArrayList<>();
        if (run != null) {
            List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets = getChangeSets(run);

            for (ChangeLogSet<? extends ChangeLogSet.Entry> changeSet : changeSets)
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

    @Extension
    @Symbol("octopusPushBuildInformation")
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
