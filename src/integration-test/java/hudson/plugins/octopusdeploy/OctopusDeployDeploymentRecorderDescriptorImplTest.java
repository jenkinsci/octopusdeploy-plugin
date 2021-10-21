package hudson.plugins.octopusdeploy;

import com.octopus.helper.BaseIntegrationTest;
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class OctopusDeployDeploymentRecorderDescriptorImplTest extends BaseIntegrationTest {

    final private OctopusDeployDeploymentRecorder.DescriptorImpl descriptor =
            new OctopusDeployDeploymentRecorder.DescriptorImpl();

    @Test
    public void doCheckProject() {
        FormValidation validation = descriptor.doCheckProject("Project1",
                JENKINS_OCTOPUS_SERVER_ID,
                space.getProperties().getId());

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void doCheckDeploymentTimeout() {
        final FormValidation validation = descriptor.doCheckDeploymentTimeout("11:00:00");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void doCheckReleaseVersionWithNullProjectFailsValidation() {
        FormValidation validation = descriptor.doCheckReleaseVersion("1.0.0",
                null,
                JENKINS_OCTOPUS_SERVER_ID,
                space.getProperties().getId());

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.WARNING);
        assertThat(validation.getMessage()).isEqualTo("Project must be set to validate release.");
    }

    @Test
    public void doCheckReleaseVersionWithEmptyProjectFailsValidation() {
        FormValidation validation =
                descriptor.doCheckReleaseVersion("1.0.0",
                        "",
                        JENKINS_OCTOPUS_SERVER_ID,
                        space.getProperties().getId());

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.WARNING);
        assertThat(validation.getMessage()).isEqualTo("Project must be set to validate release.");
    }

    @Test
    public void doCheckReleaseVersionWithoutCorrespondingProjectFailsValidation() {
        FormValidation validation =
                descriptor.doCheckReleaseVersion("1.0.0",
                        "Proj99",
                        JENKINS_OCTOPUS_SERVER_ID,
                        space.getProperties().getId());

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.WARNING);
        assertThat(StringEscapeUtils.unescapeHtml4(validation.getMessage()))
                .isEqualTo("Unable to validate release because the project 'Proj99' couldn't be found.");
    }

    @Test
    public void doCheckReleaseVersion() {
        FormValidation validation =
                descriptor.doCheckReleaseVersion("1.0.0",
                        "Project1",
                        JENKINS_OCTOPUS_SERVER_ID,
                        space.getProperties().getId());

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void doCheckEnvironment() {
        final FormValidation validation = descriptor.doCheckEnvironment("Environment1",
                JENKINS_OCTOPUS_SERVER_ID,
                space.getProperties().getId());

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void doFillEnvironmentItems() {
        List<String> existingEnvironmentNames = Arrays.asList("Environment1", "Environment2", "Environment3");

        final ComboBoxModel model = descriptor.doFillEnvironmentItems(JENKINS_OCTOPUS_SERVER_ID,
                space.getProperties().getId());

        assertThat(model).containsExactlyInAnyOrderElementsOf(existingEnvironmentNames);
    }

    @Test
    public void doFillProjectItems() {
        List<String> existingProjectNames = Arrays.asList("Project1", "Project2", "Project3");

        final ComboBoxModel model = descriptor.doFillProjectItems(JENKINS_OCTOPUS_SERVER_ID,
                space.getProperties().getId());

        assertThat(model).containsExactlyInAnyOrderElementsOf(existingProjectNames);
    }

    @Test
    public void doFillTenantTagItems() {
        final List<String> existingTenantTagNames = Arrays.asList("Tag1", "Tag2");
        final ComboBoxModel model = descriptor.doFillTenantTagItems(JENKINS_OCTOPUS_SERVER_ID,
                space.getProperties().getId());

        assertThat(model.size()).isEqualTo(existingTenantTagNames.size());
        assertThat(model)
                .containsAll(existingTenantTagNames.stream().map(tag -> "TagSet1/" + tag).collect(Collectors.toList()));
    }

    @Test
    public void doFillTenantItems() {
        List<String> existingTenantNames = Arrays.asList("Tenant1", "Tenant2", "Tenant3");

        final ComboBoxModel model = descriptor.doFillTenantItems(JENKINS_OCTOPUS_SERVER_ID,
                space.getProperties().getId());

        assertThat(model).containsExactlyInAnyOrderElementsOf(existingTenantNames);
    }
}
