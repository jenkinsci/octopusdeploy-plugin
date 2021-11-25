package hudson.plugins.octopusdeploy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class BuildInfoSummaryTest {

    @Test
    public void getIconFileNameReturnsExpectedPath() {
        final BuildInfoSummary buildInfoSummary =
                new BuildInfoSummary(BuildInfoSummary.OctopusDeployEventType.Deployment, "test");
        assertThat(buildInfoSummary.getIconFileName()).isEqualTo("/plugin/octopusdeploy/images/octopus-o.png");
    }

    @ParameterizedTest
    @MethodSource("provideNameTestArguments")
    public void getDisplayNameReturnsExpectedText(final BuildInfoSummary infoSummary, final String expectedText) {
        assertThat(infoSummary.getDisplayName()).isEqualTo(expectedText);
    }

    @ParameterizedTest
    @MethodSource("provideImageAssetTestArguments")
    public void getLabelledIconFileNameReturnsExpectedAssetName(final BuildInfoSummary infoSummary,
                                                                final String expectedAssetName) {
        final String expectedImagePath = "/plugin/octopusdeploy/images/" + expectedAssetName;
        assertThat(infoSummary.getLabelledIconFileName()).isEqualTo(expectedImagePath);
    }


    @SuppressWarnings("unused")
    private static Stream<Arguments> provideNameTestArguments() {
        return Stream.of(
                Arguments.of(
                        new BuildInfoSummary(BuildInfoSummary.OctopusDeployEventType.Deployment, "testName"),
                        "OctopusDeploy - Deployment"),
                Arguments.of(
                        new BuildInfoSummary(BuildInfoSummary.OctopusDeployEventType.Release, "testName"),
                        "OctopusDeploy - Release"));
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> provideImageAssetTestArguments() {
        return Stream.of(
                Arguments.of(
                        new BuildInfoSummary(BuildInfoSummary.OctopusDeployEventType.Deployment, "testName"),
                        "octopus-d.png"),
                Arguments.of(
                        new BuildInfoSummary(BuildInfoSummary.OctopusDeployEventType.Release, "testName"),
                        "octopus-r.png"));
    }
}
