package hudson.plugins.octopusdeploy;

import hudson.util.ListBoxModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OctopusDeployPushBuildInformationRecorderDescriptorImplTest {

    private final OctopusDeployPushBuildInformationRecorder.DescriptorImpl descriptor =
            new OctopusDeployPushBuildInformationRecorder.DescriptorImpl();

    @Test
    public void isApplicableReturnsTrue() {
        assertThat(descriptor.isApplicable(null)).isTrue();
    }

    @Test
    public void getDisplayNameReturnsFixedName() {
        assertThat(descriptor.getDisplayName()).isEqualTo("Octopus Deploy: Push build information");
    }

    @Test
    public void doFillCommentParserItemsReturnsFixedListBoxModel() {
        final ListBoxModel model = descriptor.doFillCommentParserItems();
        assertThat(model)
                .isNotEmpty()
                .flatExtracting(ListBoxModel.Option::toString)
                .containsExactly("=", "Jira=Jira", "GitHub=GitHub");
    }
}
