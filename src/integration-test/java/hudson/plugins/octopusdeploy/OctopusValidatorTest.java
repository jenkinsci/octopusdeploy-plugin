package hudson.plugins.octopusdeploy;

import com.octopus.helper.SpaceScopedClient;
import com.octopus.helper.TestHelper;
import com.octopus.sdk.domain.ProjectGroup;
import com.octopus.sdk.model.release.ReleaseResource;
import com.octopus.sdk.model.space.SpaceOverviewResource;
import com.octopus.testsupport.BaseOctopusServerEnabledTest;
import com.octopusdeploy.api.OctopusApi;
import com.octopusdeploy.api.data.Project;
import hudson.util.FormValidation;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class OctopusValidatorTest extends BaseOctopusServerEnabledTest {

    private MockedStatic<AbstractOctopusDeployRecorderPostBuildStep> mockedPostBuildStep;
    private SpaceScopedClient spaceScopedClient;
    private OctopusValidator validator;


    @BeforeEach
    public void setUp(final TestInfo testInfo) {
        spaceScopedClient = TestHelper.buildSpaceScopedClientForTesting(httpClient,
                server,
                TestHelper.generateSpaceName(testInfo.getDisplayName()));
        validator = new OctopusValidator(new OctopusApi(server.getOctopusUrl(), server.getApiKey())
                .forSpace(spaceScopedClient.getSpaceId()));
        mockedPostBuildStep = Mockito.mockStatic(AbstractOctopusDeployRecorderPostBuildStep.class);
    }

    @AfterEach
    public void cleanUp() throws IOException {
        mockedPostBuildStep.close();

        if (spaceScopedClient.getRepository() != null && spaceScopedClient.getSpace() != null) {
            final SpaceOverviewResource resource = spaceScopedClient.getSpace().getProperties();
            resource.setTaskQueueStopped(true);
            spaceScopedClient.getRepository().spaces().update(resource);
            spaceScopedClient.getRepository().spaces().delete(resource);
        }
    }

    @Test
    public void validateProjectEmptyProjectFailsValidation() {
        final FormValidation validation = validator.validateProject("");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage())).isEqualTo("Please provide a project name.");
    }

    @Test
    public void validateProjectWithoutCorrespondingProjectFailsValidation() {
        final FormValidation validation = validator.validateProject("Proj1");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.WARNING);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage()))
                .isEqualTo("Project 'Proj1' doesn't exist. " +
                        "If this field is computed you can disregard this warning.");
    }

    @Test
    public void validateProjectWithoutCorrespondingProjectNameCaseFailsValidation() {
        spaceScopedClient.createProject("Proj1", "ProjGroup1");

        final FormValidation validation = validator.validateProject("proj1");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.WARNING);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage()))
                .isEqualTo("Project name case does not match. Did you mean 'Proj1'?");
    }

    @Test
    public void validateProjectWithCorrespondingProjectNamePassesValidation() {
        spaceScopedClient.createProject("Proj1", "ProjGroup1");
        final FormValidation validation = validator.validateProject("Proj1");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void validateChannelEmptyChannelPassesValidation() {
        final FormValidation validation = validator.validateChannel("", "Proj1");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void validateChannelEmptyProjectFailsValidation() {
        final FormValidation validation = validator.validateChannel("Channel1", "");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.WARNING);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage())).isEqualTo("Project must be set to validate this field.");
    }

    @Test
    public void validateChannelWithoutCorrespondingProjectFailsValidation() {
        final FormValidation validation = validator.validateChannel("Channel1", "Proj1");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.WARNING);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage()))
                .isEqualTo("Unable to validate channel because the project 'Proj1' couldn't be found.");
    }

    @Test
    public void validateChannelWithoutCorrespondingChannelFailsValidation() {
        spaceScopedClient.createProject("Proj1", "ProjGroup1");

        final FormValidation validation = validator.validateChannel("Channel1", "Proj1");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.WARNING);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage()))
                .isEqualTo("Channel 'Channel1' doesn't exist. " +
                        "If this field is computed you can disregard this warning.");
    }

    @Test
    public void validateChannelWithCorrespondingChannelAndProjectPassesValidation() {
        final com.octopus.sdk.domain.Project project =
                spaceScopedClient.createProject("Proj1", "ProjGroup1");
        spaceScopedClient.createChannel("Channel1", project.getProperties().getId());

        final FormValidation validation = validator.validateChannel("Channel1", "Proj1");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void validateEnvironmentWithEmptyEnvironmentNameFailsValidation() {
        final FormValidation validation = validator.validateEnvironment("");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage())).isEqualTo("Please provide an environment name.");
    }

    @Test
    public void validateEnvironmentWithoutEnvironmentsFailsValidation() {
        final FormValidation validation = validator.validateEnvironment("Env1");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.WARNING);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage()))
                .isEqualTo("Environment 'Env1' doesn't exist. " +
                        "If this field is computed you can disregard this warning.");
    }

    @Test
    public void validateEnvironmentWithUnmatchedEnvironmentFailsValidation() {
        spaceScopedClient.createEnvironment("Env1");

        final FormValidation validation = validator.validateEnvironment("env1");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.WARNING);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage()))
                .isEqualTo("Environment name case does not match. Did you mean 'Env1'?");
    }

    @Test
    public void validateEnvironmentWithCorrespondingEnvironmentPassesValidation() {
        spaceScopedClient.createEnvironment("Env1");

        final FormValidation validation = validator.validateEnvironment("Env1");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void validateReleaseWithEmptyReleaseVersionFailsValidation() {
        final FormValidation validation = validator.validateRelease("",
                new Project("id", "name"),
                OctopusValidator.ReleaseExistenceRequirement.MustNotExist);

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage())).isEqualTo("Please provide a release version.");
    }

    @Test
    public void validateReleaseWhereReleaseMustExistFailsValidation() {
        final ProjectGroup projGroup = spaceScopedClient.createProjectGroup("ProjGroup1");
        final com.octopus.sdk.domain.Project newProject =
                spaceScopedClient.createProject("Proj1", projGroup.getProperties().getId());
        final Project project = new Project(newProject.getProperties().getId(), newProject.getProperties().getName());

        final FormValidation validation = validator.validateRelease("1.0.0",
                project,
                OctopusValidator.ReleaseExistenceRequirement.MustExist);

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.WARNING);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage()))
                .isEqualTo("Release 1.0.0 doesn't exist for project 'Proj1'. " +
                        "If this field is computed you can disregard this warning.");
    }

    @Test
    public void validateReleaseWhereReleaseMustExistPassesValidation() throws IOException {
        final com.octopus.sdk.domain.Project newProject =
                spaceScopedClient.createProject("Proj1", "ProjGroup1");
        spaceScopedClient.getSpace().releases().create(new ReleaseResource("1.0.0", newProject.getProperties().getId()));
        final Project project = new Project(newProject.getProperties().getId(), newProject.getProperties().getName());

        final FormValidation validation = validator.validateRelease("1.0.0",
                project,
                OctopusValidator.ReleaseExistenceRequirement.MustExist);

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void validateReleaseWhereReleaseMustNotExistFailsValidation() throws IOException {
        final com.octopus.sdk.domain.Project newProject =
                spaceScopedClient.createProject("Proj1", "ProjGroup1");
        spaceScopedClient.getSpace().releases().create(new ReleaseResource("1.0.0", newProject.getProperties().getId()));
        final Project project = new Project(newProject.getProperties().getId(), newProject.getProperties().getName());

        final FormValidation validation = validator.validateRelease("1.0.0",
                project,
                OctopusValidator.ReleaseExistenceRequirement.MustNotExist);

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage()))
                .isEqualTo("Release 1.0.0 already exists for project 'Proj1'!");
    }

    @Test
    public void validateReleaseWhereReleaseMustNotExistPassesValidation() {
        final com.octopus.sdk.domain.Project newProject =
                spaceScopedClient.createProject("Proj1", "ProjGroup1");
        final Project project = new Project(newProject.getProperties().getId(), newProject.getProperties().getName());

        final FormValidation validation = validator.validateRelease("1.0.0",
                project,
                OctopusValidator.ReleaseExistenceRequirement.MustNotExist);

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void validateServerIdWithNullServerIdFailsValidation() {
        final FormValidation validation = OctopusValidator.validateServerId(null);

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage())).isEqualTo("Please select an instance of Octopus Deploy.");
    }

    @Test
    public void validateServerIdWithEmptyServerIdFailsValidation() {
        final FormValidation validation = OctopusValidator.validateServerId("");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage())).isEqualTo("Please select an instance of Octopus Deploy.");
    }

    @Test
    public void validateServerIdWithDefaultServerIdPassesValidation() {
        final FormValidation validation = OctopusValidator.validateServerId("default");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void validateServerIdWithNoConfiguredServersFailsValidation() {
        mockedPostBuildStep
                .when(AbstractOctopusDeployRecorderPostBuildStep::getOctopusDeployServersIds)
                .thenReturn(Collections.EMPTY_LIST);

        final FormValidation validation = OctopusValidator.validateServerId("someId");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage())).isEqualTo("There are no Octopus Deploy servers configured.");
    }

    @Test
    public void validateServerIdWithNoMatchingServersFailsValidation() {
        mockedPostBuildStep
                .when(AbstractOctopusDeployRecorderPostBuildStep::getOctopusDeployServersIds)
                .thenReturn(Collections.singletonList("someId"));

        final FormValidation validation = OctopusValidator.validateServerId("otherId");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage()))
                .isEqualTo("There are no Octopus Deploy servers configured with this Server Id.");
    }

    @Test
    public void validateServerIdWithMatchingServerPassesValidation() {
        mockedPostBuildStep
                .when(AbstractOctopusDeployRecorderPostBuildStep::getOctopusDeployServersIds)
                .thenReturn(Collections.singletonList("someId"));

        final FormValidation validation = OctopusValidator.validateServerId("someId");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

}
