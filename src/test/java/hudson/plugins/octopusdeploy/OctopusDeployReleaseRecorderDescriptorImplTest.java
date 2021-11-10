package hudson.plugins.octopusdeploy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OctopusDeployReleaseRecorderDescriptorImplTest {

    private final OctopusDeployReleaseRecorder.DescriptorImpl descriptor =
            new OctopusDeployReleaseRecorder.DescriptorImpl();

    @Test
    public void isApplicableReturnsTrue() {
        assertThat(descriptor.isApplicable(null)).isTrue();
    }

    @Test
    public void getDisplayNameReturnsFixedName() {
        assertThat(descriptor.getDisplayName()).isEqualTo("Octopus Deploy: Create Release");
    }

}
