package hudson.plugins.octopusdeploy;

import jenkins.model.Jenkins;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OctopusDeployDeploymentRecorderDescriptorImplTest {

    private final OctopusDeployDeploymentRecorder.DescriptorImpl descriptor =
            new OctopusDeployDeploymentRecorder.DescriptorImpl();

    @BeforeAll
    static void setUp() {
        final Jenkins jenkins = mock(Jenkins.class);
        final MockedStatic<Jenkins> jenkinsMockedStatic = Mockito.mockStatic(Jenkins.class);

        jenkinsMockedStatic.when(Jenkins::get).thenReturn(jenkins);
        when(Jenkins.getInstanceOrNull()).thenReturn(jenkins);
    }

    @Test
    public void isApplicableReturnsTrue() {
        assertThat(descriptor.isApplicable(null)).isTrue();
    }

    @Test
    public void getDisplayNameReturnsFixedName() {
        assertThat(descriptor.getDisplayName()).isEqualTo("Octopus Deploy: Deploy Release");
    }
}
