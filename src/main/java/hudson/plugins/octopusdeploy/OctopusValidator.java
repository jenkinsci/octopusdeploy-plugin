package hudson.plugins.octopusdeploy;

import com.octopusdeploy.api.data.Project;
import com.octopusdeploy.api.data.Release;
import com.octopusdeploy.api.*;
import hudson.util.FormValidation;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

/**
 * Validations on input for Octopus Deploy.
 */
public class OctopusValidator {
    private final OctopusApi api;
    
    public OctopusValidator(OctopusApi api) {
        this.api = api;
    }

    /**
     * Provides validation on a Project.
     * Validates:
     *  Project is not empty.
     *  Project exists in Octopus.
     *  Project is appropriate case.
     * @param projectName name of the project to validate.
     * @return a form validation.
     */
    public FormValidation validateProject(String projectName) {
        if (projectName.isEmpty()) {
            return FormValidation.error("Please provide a project name.");
        }
        try {
            com.octopusdeploy.api.data.Project p = api.getProjectsApi().getProjectByName(projectName, true);
            if (p == null)
            {
                return FormValidation.warning("Project '%s' doesn't exist. If this field is computed you can disregard this warning.", projectName);
            }
            if (!projectName.equals(p.getName()))
            {
                return FormValidation.warning("Project name case does not match. Did you mean '%s'?", p.getName());
            }
        } catch (IllegalArgumentException ex) {
            return FormValidation.error(ex.getMessage());
        } catch (IOException ex) {
            return FormValidation.error(ex.getMessage());
        }
        return FormValidation.ok();
    }
    
    /**
     * Provides validation on a Channel.
     * Validates:
     *  Project is not empty.
     *  Project exists in Octopus.
     *  Project is appropriate case.
     *  Channel is either empty or exists in Octopus
     * @param channelName name of the channel to validate
     * @param projectName name of the project to validate.
     * @return a form validation.
     */
    public FormValidation validateChannel(String channelName, String projectName) {
        if (channelName != null && !channelName.isEmpty()) {
            if (projectName == null || projectName.isEmpty()) {
                return FormValidation.warning("Project must be set to validate this field.");
            }
            com.octopusdeploy.api.data.Project project;
            com.octopusdeploy.api.data.Channel channel;
            try {
                project = api.getProjectsApi().getProjectByName(projectName);
                if (project != null) {
                    channel = api.getChannelsApi().getChannelByName(project.getId(), channelName);
                    if (channel == null) {
                        return FormValidation.warning("Channel '%s' doesn't exist. If this field is computed you can disregard this warning.", channelName);
                    }
                }
                else
                {
                    return FormValidation.warning("Unable to validate channel because the project '%s' couldn't be found.", projectName);
                }
            } catch (IllegalArgumentException ex) {
                return FormValidation.warning("Unable to validate field - " + ex.getMessage());
            } catch (IOException ex) {
                return FormValidation.warning("Unable to validate field - " + ex.getMessage());
            }
        }
        return FormValidation.ok();
    }
    
    /**
     * Provides validation on an environment.
     * Validates:
     *  Environment is not empty.
     *  Environment exists in Octopus.
     *  Environment is appropriate case.
     * @param environmentName the name of the environment to validate.
     * @return a form validation.
     */
    public FormValidation validateEnvironment(String environmentName) {
        if (environmentName.isEmpty()) {
            return FormValidation.error("Please provide an environment name.");
        }
        try {
            com.octopusdeploy.api.data.Environment env = api.getEnvironmentsApi().getEnvironmentByName(environmentName, true);
            if (env == null)
            {
                return FormValidation.warning("Environment '%s' doesn't exist. If this field is computed you can disregard this warning.", environmentName);
            }
            if (!environmentName.equals(env.getName()))
            {
                return FormValidation.warning("Environment name case does not match. Did you mean '%s'?", env.getName());
            }
        } catch (IllegalArgumentException ex) {
            return FormValidation.error(ex.getMessage());
        } catch (IOException ex) {
            return FormValidation.error(ex.getMessage());
        }
        return FormValidation.ok();
    }
    
    /**
     * Provides validation on releases.
     * Validates:
     *  The project is set.
     *  The release is not empty.
     *  The release conforms to the existence check requirement.
     * @param releaseVersion the release version.
     * @param project the project that this release is for.
     * @param existenceCheckReq the requirement for the existence of the release.
     * @return FormValidation response
     */
    public FormValidation validateRelease(String releaseVersion, Project project, ReleaseExistenceRequirement existenceCheckReq) {
        if (releaseVersion.isEmpty()) {
            return FormValidation.error("Please provide a release version.");
        }
        try {
            Set<Release> releases = api.getReleasesApi().getReleasesForProject(project.getId());

            boolean found = false;
            for (Release release : releases) {
                if (releaseVersion.equals(release.getVersion()) ) {
                    found = true;
                    break;
                }
            }
            if (found && existenceCheckReq == ReleaseExistenceRequirement.MustNotExist) {
                return FormValidation.error("Release %s already exists for project '%s'!", releaseVersion, project.getName());
            }
            if (!found && existenceCheckReq == ReleaseExistenceRequirement.MustExist) {
                return FormValidation.warning("Release %s doesn't exist for project '%s'. If this field is computed you can disregard this warning.", releaseVersion, project.getName());
            }
        } catch (IllegalArgumentException ex) {
            return FormValidation.error(ex.getMessage());
        } catch (IOException ex) {
            return FormValidation.error(ex.getMessage());
        }
        return FormValidation.ok();
    }

    public static FormValidation validateServerId(String serverId) {
        if (serverId==null || serverId.isEmpty()) {
            return FormValidation.error("Please select an instance of Octopus Deploy.");
        }
        if(serverId.equals("default")) {
            return FormValidation.ok();
        }
        List<String> ids = AbstractOctopusDeployRecorderPostBuildStep.getOctopusDeployServersIds();
        if (ids.isEmpty()){
            return FormValidation.error("There are no Octopus Deploy servers configured.");
        }
        if (!ids.contains(serverId)) {
            return FormValidation.error("There are no Octopus Deploy servers configured with this Server Id.");
        }
        return FormValidation.ok();
    }

    /**
     * Whether or not a release must exist or must not exist depending on the operation being done.
     */
    public enum ReleaseExistenceRequirement {
        MustExist, MustNotExist
    }

    public static FormValidation validateDirectory(String directoryPath) {
        if (directoryPath != null) {
            directoryPath = directoryPath.trim();
            if (!directoryPath.isEmpty() && !isValidDirectory(directoryPath)) {
                return FormValidation.error("This is not a path to a directory");
            }
        }

        return FormValidation.ok();
    }

    public static FormValidation validateDeploymentTimeout(String deploymentTimeout) {
        if (deploymentTimeout != null) {
            deploymentTimeout = deploymentTimeout.trim();
            if (!deploymentTimeout.isEmpty() && !isValidTimeSpan(deploymentTimeout)) {
                return FormValidation.error("This is not a valid deployment timeout it should be in the format HH:mm:ss");
            }
        }

        return FormValidation.ok();
    }

    public static Boolean isValidTimeSpan(String deploymentTimeout)
    {
        try {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
            dtf.parse(deploymentTimeout);
        } catch (DateTimeParseException ex) {
            return false;
        }
        return true;
    }

    public static Boolean isValidDirectory(String path) {
        File f = new File(path);
        return f.exists() && f.isDirectory();
    }
}
