package hudson.plugins.octopusdeploy;

import com.octopus.helper.BaseIntegrationTest;
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;


class OctopusDeployReleaseRecorderDescriptorImplITest extends BaseIntegrationTest {

    private final OctopusDeployReleaseRecorder.DescriptorImpl descriptor =
            new OctopusDeployReleaseRecorder.DescriptorImpl();

    @Test
    public void doCheckProject() {
        FormValidation validation = descriptor.doCheckProject("Project1",
                JENKINS_OCTOPUS_SERVER_ID,
                space.getProperties().getId());

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void doCheckChannel() {
        FormValidation validation = descriptor.doCheckChannel("Channel1",
                "Project1",
                JENKINS_OCTOPUS_SERVER_ID,
                space.getProperties().getId());

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
                descriptor.doCheckReleaseVersion("9.9.9", // release must not exist
                        "Project1",
                        JENKINS_OCTOPUS_SERVER_ID,
                        space.getProperties().getId());

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void doCheckReleaseNotesFileWithReleaseNotesPassesValidation() {
        FormValidation validation = descriptor.doCheckReleaseNotesFile("Release Notes");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void doCheckReleaseNotesFileWithEmptyReleaseNotesFailsValidation() {
        FormValidation validation = descriptor.doCheckReleaseNotesFile("");

        assertThat(validation.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(validation.getMessage()).isEqualTo("Please provide a project notes file.");
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
        List<String> exitingEnvironmentNames = Arrays.asList("Environment1", "Environment2", "Environment3");

        final ComboBoxModel model = descriptor.doFillEnvironmentItems(JENKINS_OCTOPUS_SERVER_ID,
                space.getProperties().getId());

        assertThat(model).containsExactlyInAnyOrderElementsOf(exitingEnvironmentNames);
    }

    @Test
    public void doFillProjectItems() {
        List<String> existingProjectNames = Arrays.asList("Project1", "Project2", "Project3");

        final ComboBoxModel model = descriptor.doFillProjectItems(JENKINS_OCTOPUS_SERVER_ID,
                space.getProperties().getId());

        assertThat(model).containsExactlyInAnyOrderElementsOf(existingProjectNames);
    }

    @Test
    public void doFillChannelItems() {
        List<String> existingChannelNames = Arrays.asList("Channel1", "Channel2", "Channel3");

        final ComboBoxModel model = descriptor.doFillChannelItems("Project1",
                JENKINS_OCTOPUS_SERVER_ID,
                space.getProperties().getId());

        assertThat(model.size()).isEqualTo(existingChannelNames.size() + 1); // Increase by 1 for default channel
        assertThat(model).containsAll(existingChannelNames);
    }

    @Test
    public void doFillTenantTagItems() {
        final List<String> existingTagNames = Arrays.asList("Tag1", "Tag2");
        final ComboBoxModel model = descriptor.doFillTenantTagItems(JENKINS_OCTOPUS_SERVER_ID,
                space.getProperties().getId());

        assertThat(model.size()).isEqualTo(existingTagNames.size());
        assertThat(model)
                .containsAll(existingTagNames.stream().map(tag -> "TagSet1/" + tag).collect(Collectors.toList()));
    }

    @Test
    public void doFillTenantItems() {
        List<String> existingTenantNames = Arrays.asList("Tenant1", "Tenant2", "Tenant3");

        final ListBoxModel model = descriptor.doFillTenantItems(JENKINS_OCTOPUS_SERVER_ID,
                space.getProperties().getId());

        assertThat(model.size()).isEqualTo(existingTenantNames.size());
        assertThat(model).extracting("name").containsAll(existingTenantNames);
        assertThat(model).extracting("value").containsAll(existingTenantNames);
    }

}
