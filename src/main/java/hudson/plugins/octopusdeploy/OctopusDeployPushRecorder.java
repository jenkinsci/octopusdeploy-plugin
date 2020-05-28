package hudson.plugins.octopusdeploy;

import com.google.common.base.Splitter;
import com.google.inject.Guice;
import com.google.inject.Inject;
import hudson.*;
import hudson.model.*;
import hudson.plugins.octopusdeploy.constants.OctoConstants;
import hudson.plugins.octopusdeploy.services.FileService;
import hudson.plugins.octopusdeploy.services.ServiceModule;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;
import hudson.util.VariableResolver;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.types.Commandline;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.FileSystem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class OctopusDeployPushRecorder extends AbstractOctopusDeployRecorderBuildStep implements Serializable {

    private transient FileService fileService;

    private final String packagePaths;
    public String getPackagePaths() { return packagePaths; }

    private final OverwriteMode overwriteMode;
    public OverwriteMode getOverwriteMode() { return overwriteMode; }

    @DataBoundConstructor
    public OctopusDeployPushRecorder(String serverId, String spaceId, String toolId, String packagePaths,
                                     OverwriteMode overwriteMode, Boolean verboseLogging, String additionalArgs) {
        this.serverId = serverId.trim();
        this.spaceId = spaceId.trim();
        this.toolId = toolId.trim();
        this.packagePaths = packagePaths.trim();
        this.overwriteMode = overwriteMode;
        this.verboseLogging = verboseLogging;
        this.additionalArgs = additionalArgs.trim();
    }

    @Inject
    public void setFileService(FileService fileService) {
        checkNotNull(fileService, "fileService cannot be null");
        this.fileService = fileService;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        if (fileService == null)
        {
            Guice.createInjector(new ServiceModule()).injectMembers(this);
        }

        boolean success = true;
        Log log = new Log(listener);
        if (Result.FAILURE.equals(build.getResult())) {
            log.info("Not packaging the application due to job being in FAILED state.");
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

        String packagePathPattern = envInjector.injectEnvironmentVariableValues(this.packagePaths);
        //logStartHeader

         /*
            Get the list of matching files that need to be uploaded
         */
        final List<FilePath> files = new ArrayList<>();

        final Iterable<String> patternSplit = Splitter.on("\n")
                .trimResults()
                .omitEmptyStrings()
                .split(packagePathPattern);
        FilePath ws = build.getWorkspace();
        for (final String pattern : patternSplit) {
            final List<FilePath> matchingFiles = fileService.getMatchingFile(ws, pattern, log);
            /*
                Don't add duplicates
             */
            for (final FilePath file : matchingFiles) {
                files.add(file);
                if (file != null) {
                    final FilePath existing = CollectionUtils.find(files, new Predicate<FilePath>() {
                        @Override
                        public boolean evaluate(FilePath existingFile) {
                            return existingFile.getBaseName().equals(file.getBaseName());
                        }
                    });

                    if (existing == null) {
                        files.add(file);
                    }
                }
            }
        }

        try {
            final List<String> commands = buildCommands(envInjector, files, ws);
            final Boolean[] masks = getMasks(commands, OctoConstants.Commands.Arguments.MaskedArguments);
            Result result = launchOcto(build.getBuiltOn(), launcher, commands, masks, envVars, listener);
            success = result.equals(Result.SUCCESS);
        } catch (Exception ex) {
            log.fatal("Failed to push the packages: " + ex.getMessage());
            success = false;
        }

        return success;
    }

    private List<String> buildCommands(final EnvironmentVariableValueInjector envInjector, final List<FilePath> files, FilePath workspace) throws IOException, InterruptedException {
        final List<String> commands = new ArrayList<>();

        OctopusDeployServer server = getOctopusDeployServer(this.serverId);
        String serverUrl = server.getUrl();
        String apiKey = server.getApiKey().getPlainText();
        boolean ignoreSslErrors = server.getIgnoreSslErrors();
        OverwriteMode overwriteMode = this.overwriteMode;
        Boolean verboseLogging = this.verboseLogging;
        String additionalArgs = envInjector.injectEnvironmentVariableValues(this.additionalArgs);

        checkState(StringUtils.isNotBlank(serverUrl), String.format(OctoConstants.Errors.INPUT_CANNOT_BE_BLANK_MESSAGE_FORMAT, "Octopus URL"));
        checkState(StringUtils.isNotBlank(apiKey), String.format(OctoConstants.Errors.INPUT_CANNOT_BE_BLANK_MESSAGE_FORMAT, "API Key"));
        checkState(!files.isEmpty(), String.format("The pattern \n%s\n failed to match any files", packagePaths));

        commands.add("push");

        commands.add("--server");
        commands.add(serverUrl);

        commands.add("--apiKey");
        commands.add(apiKey);

        if (StringUtils.isNotBlank(spaceId)) {
            commands.add("--space");
            commands.add(spaceId);
        }

        for (final FilePath file : files) {
            commands.add("--package");
            commands.add(file.absolutize().getRemote());
        }

        if (overwriteMode != OverwriteMode.FailIfExists) {
            commands.add("--overwrite-mode");
            commands.add(overwriteMode.name());
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

    @Extension
    public static final class DescriptorImpl extends AbstractOctopusDeployDescriptorImplStep {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Octopus Deploy: Push packages";
        }
    }
}
