package com.octopus.helper;

import com.octopus.testsupport.BaseOctopusServerEnabledTest;
import hudson.plugins.octopusdeploy.AbstractOctopusDeployRecorderPostBuildStep;
import hudson.plugins.octopusdeploy.OctopusDeployServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class BaseRecorderTest extends BaseOctopusServerEnabledTest {

    public static final String JENKINS_OCTOPUS_SERVER_ID = "default";

    private MockedStatic<AbstractOctopusDeployRecorderPostBuildStep> mockedPostBuildStep;
    public SpaceScopedClient spaceScopedClient;

    @BeforeEach
    public void setUp(final TestInfo testInfo) {
        spaceScopedClient = TestHelper.buildSpaceScopedClientForTesting(httpClient,
                server,
                TestHelper.generateSpaceName(testInfo.getDisplayName()));

        mockedPostBuildStep = Mockito.mockStatic(AbstractOctopusDeployRecorderPostBuildStep.class);
        mockedPostBuildStep
                .when(() ->
                        AbstractOctopusDeployRecorderPostBuildStep.getOctopusDeployServer(JENKINS_OCTOPUS_SERVER_ID))
                .thenReturn(new OctopusDeployServer(JENKINS_OCTOPUS_SERVER_ID,
                        server.getOctopusUrl(), server.getApiKey(), true));
    }

    @AfterEach
    public void cleanUp() {
        mockedPostBuildStep.close();
        TestHelper.deleteTestingSpace(spaceScopedClient);
    }
}
