package hudson.plugins.octopusdeploy;

import com.octopusdeploy.api.*;
import hudson.util.FormValidation;
import java.io.IOException;
import java.util.Set;

/**
 * Validations on input for OctopusDeploy.
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
            com.octopusdeploy.api.Project p = api.getProjectByName(projectName, true);
            if (p == null)
            {
                return FormValidation.error("Project not found.");
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
            com.octopusdeploy.api.Environment env = api.getEnvironmentByName(environmentName, true);
            if (env == null)
            {
                return FormValidation.error("Environment not found.");
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
     * @param projectId the project's Id that this release is for.
     * @param existanceCheckReq the requirement for the existence of the release.
     * @return FormValidation response
     */
    public FormValidation validateRelease(String releaseVersion, String projectId, ReleaseExistenceRequirement existanceCheckReq) {
        if (releaseVersion.isEmpty()) {
            return FormValidation.error("Please provide a release version.");
        }
        try {
            Set<Release> releases = api.getReleasesForProject(projectId);
            boolean found = false;
            for (Release release : releases) {
                if (releaseVersion.equals(release.getVersion()) ) {
                    found = true;
                    break;
                }
            }
            if (found && existanceCheckReq == ReleaseExistenceRequirement.MustNotExist) {
                return FormValidation.error("Release %s already exists for project %s!", releaseVersion, projectId);
            }
            if (!found && existanceCheckReq == ReleaseExistenceRequirement.MustExist) {
                return FormValidation.error("Release %s doesn't exist for project %s!", releaseVersion, projectId);
            }
        } catch (IllegalArgumentException ex) {
            return FormValidation.error(ex.getMessage());
        } catch (IOException ex) {
            return FormValidation.error(ex.getMessage());
        }
        return FormValidation.ok();
    }
    
    /**
     * Whether or not a release must exist or must not exist depending on the operation being done.
     */
    public enum ReleaseExistenceRequirement {
        MustExist, MustNotExist
    }
}
