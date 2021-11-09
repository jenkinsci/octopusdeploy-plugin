package hudson.plugins.octopusdeploy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OctopusDeployPushRecorderDescriptorImplTest {

    private final OctopusDeployPushRecorder.DescriptorImpl descriptor =
        new OctopusDeployPushRecorder.DescriptorImpl();

    @Test
    public void isApplicableReturnsTrue() {
        assertThat(descriptor.isApplicable(null)).isTrue();
    }

    @Test
    public void getDisplayNameReturnsFixedName() {
        assertThat(descriptor.getDisplayName()).isEqualTo("Octopus Deploy: Push packages");
    }
}
