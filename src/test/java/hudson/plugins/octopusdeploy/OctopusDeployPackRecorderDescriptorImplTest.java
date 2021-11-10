package hudson.plugins.octopusdeploy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OctopusDeployPackRecorderDescriptorImplTest {

    private final OctopusDeployPackRecorder.DescriptorImpl descriptor =
            new OctopusDeployPackRecorder.DescriptorImpl();

    @Test
    public void isApplicableReturnsTrue() {
        assertThat(descriptor.isApplicable(null)).isTrue();
    }

    @Test
    public void getDisplayNameReturnsFixedName() {
        assertThat(descriptor.getDisplayName()).isEqualTo("Octopus Deploy: Package application");
    }
}
