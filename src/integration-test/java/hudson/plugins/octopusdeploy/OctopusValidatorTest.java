package hudson.plugins.octopusdeploy;

import com.octopus.helper.BaseIntegrationTest;
import com.octopusdeploy.api.OctopusApi;
import com.octopusdeploy.api.data.Project;
import hudson.util.FormValidation;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class OctopusValidatorTest extends BaseIntegrationTest {

    private OctopusValidator validator;

    @BeforeEach
    public void localSetup() {
        validator = new OctopusValidator(new OctopusApi(server.getOctopusUrl(), server.getApiKey())
                .forSpace(space.getProperties().getId()));
    }

    @Test
    public void validateProjectEmptyProjectFailsValidation() {
        final FormValidation validation = validator.validateProject("");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage()))
                .isEqualTo("Please provide a project name.");
    }

    @Test
    public void validateProjectWithoutCorrespondingProjectFailsValidation() {
        final FormValidation validation = validator.validateProject("Proj99");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.WARNING);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage()))
                .isEqualTo("Project 'Proj99' doesn't exist. " +
                        "If this field is computed you can disregard this warning.");
    }

    @Test
    public void validateProjectWithoutCorrespondingProjectNameCaseFailsValidation() {
        final FormValidation validation = validator.validateProject("project1"); // unmatched case

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.WARNING);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage()))
                .isEqualTo("Project name case does not match. Did you mean 'Project1'?");
    }

    @Test
    public void validateProjectWithCorrespondingProjectNamePassesValidation() {
        final FormValidation validation = validator.validateProject("Project1");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void validateChannelEmptyChannelPassesValidation() {
        final FormValidation validation = validator.validateChannel("", "Project1");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void validateChannelEmptyProjectFailsValidation() {
        final FormValidation validation = validator.validateChannel("Channel1", "");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.WARNING);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage()))
                .isEqualTo("Project must be set to validate this field.");
    }

    @Test
    public void validateChannelWithoutCorrespondingProjectFailsValidation() {
        final FormValidation validation = validator.validateChannel("Channel1", "Proj99");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.WARNING);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage()))
                .isEqualTo("Unable to validate channel because the project 'Proj99' couldn't be found.");
    }

    @Test
    public void validateChannelWithoutCorrespondingChannelFailsValidation() {
        final FormValidation validation = validator.validateChannel("Channel99", "Project1");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.WARNING);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage()))
                .isEqualTo("Channel 'Channel99' doesn't exist. " +
                        "If this field is computed you can disregard this warning.");
    }

    @Test
    public void validateChannelWithCorrespondingChannelAndProjectPassesValidation() {
        final FormValidation validation = validator.validateChannel("Channel1", "Project1");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void validateEnvironmentWithEmptyEnvironmentNameFailsValidation() {
        final FormValidation validation = validator.validateEnvironment("");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage()))
                .isEqualTo("Please provide an environment name.");
    }

    @Test
    public void validateEnvironmentWithoutEnvironmentsFailsValidation() {
        final FormValidation validation = validator.validateEnvironment("Env99");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.WARNING);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage()))
                .isEqualTo("Environment 'Env99' doesn't exist. " +
                        "If this field is computed you can disregard this warning.");
    }

    @Test
    public void validateEnvironmentWithUnmatchedEnvironmentFailsValidation() {
        final FormValidation validation
                = validator.validateEnvironment("environment1"); // unmatched case

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.WARNING);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage()))
                .isEqualTo("Environment name case does not match. Did you mean 'Environment1'?");
    }

    @Test
    public void validateEnvironmentWithCorrespondingEnvironmentPassesValidation() {
        final FormValidation validation = validator.validateEnvironment("Environment1");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void validateReleaseWithEmptyReleaseVersionFailsValidation() {
        final FormValidation validation = validator.validateRelease("",
                new Project("id", "name"),
                OctopusValidator.ReleaseExistenceRequirement.MustNotExist);

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage()))
                .isEqualTo("Please provide a release version.");
    }

    @Test
    public void validateReleaseWhereReleaseMustExistFailsValidation() throws IOException {
        final com.octopus.sdk.domain.Project existingProject = space.projects().getByName("Project1").get();
        final Project project =
                new Project(existingProject.getProperties().getId(), existingProject.getProperties().getName());

        final FormValidation validation = validator.validateRelease("9.9.9",
                project,
                OctopusValidator.ReleaseExistenceRequirement.MustExist);

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.WARNING);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage()))
                .isEqualTo("Release 9.9.9 doesn't exist for project 'Project1'. " +
                        "If this field is computed you can disregard this warning.");
    }

    @Test
    public void validateReleaseWhereReleaseMustExistPassesValidation() throws IOException {
        final com.octopus.sdk.domain.Project existingProject = space.projects().getByName("Project1").get();
        final Project project =
                new Project(existingProject.getProperties().getId(), existingProject.getProperties().getName());

        final FormValidation validation = validator.validateRelease("1.0.0",
                project,
                OctopusValidator.ReleaseExistenceRequirement.MustExist);

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void validateReleaseWhereReleaseMustNotExistFailsValidation() throws IOException {
        final com.octopus.sdk.domain.Project existingProject = space.projects().getByName("Project1").get();
        final Project project =
                new Project(existingProject.getProperties().getId(), existingProject.getProperties().getName());

        final FormValidation validation = validator.validateRelease("1.0.0",
                project,
                OctopusValidator.ReleaseExistenceRequirement.MustNotExist);

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage()))
                .isEqualTo("Release 1.0.0 already exists for project 'Project1'!");
    }

    @Test
    public void validateReleaseWhereReleaseMustNotExistPassesValidation() throws IOException {
        final com.octopus.sdk.domain.Project existingProject = space.projects().getByName("Project1").get();
        final Project project =
                new Project(existingProject.getProperties().getId(), existingProject.getProperties().getName());

        final FormValidation validation = validator.validateRelease("9.9.9",
                project,
                OctopusValidator.ReleaseExistenceRequirement.MustNotExist);

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void validateServerIdWithNullServerIdFailsValidation() {
        final FormValidation validation = OctopusValidator.validateServerId(null);

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage()))
                .isEqualTo("Please select an instance of Octopus Deploy.");
    }

    @Test
    public void validateServerIdWithEmptyServerIdFailsValidation() {
        final FormValidation validation = OctopusValidator.validateServerId("");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage()))
                .isEqualTo("Please select an instance of Octopus Deploy.");
    }

    @Test
    public void validateServerIdWithDefaultServerIdPassesValidation() {
        final FormValidation validation = OctopusValidator.validateServerId("default");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void validateServerIdWithNoConfiguredServersFailsValidation() {
        postBuildStepMockedStatic
                .when(AbstractOctopusDeployRecorderPostBuildStep::getOctopusDeployServersIds)
                .thenReturn(Collections.EMPTY_LIST);

        final FormValidation validation = OctopusValidator.validateServerId("someId");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage()))
                .isEqualTo("There are no Octopus Deploy servers configured.");
    }

    @Test
    public void validateServerIdWithNoMatchingServersFailsValidation() {
        postBuildStepMockedStatic
                .when(AbstractOctopusDeployRecorderPostBuildStep::getOctopusDeployServersIds)
                .thenReturn(Collections.singletonList("someId"));

        final FormValidation validation = OctopusValidator.validateServerId("otherId");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage()))
                .isEqualTo("There are no Octopus Deploy servers configured with this Server Id.");
    }

    @Test
    public void validateServerIdWithMatchingServerPassesValidation() {
        postBuildStepMockedStatic
                .when(AbstractOctopusDeployRecorderPostBuildStep::getOctopusDeployServersIds)
                .thenReturn(Collections.singletonList("someId"));

        final FormValidation validation = OctopusValidator.validateServerId("someId");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

}
