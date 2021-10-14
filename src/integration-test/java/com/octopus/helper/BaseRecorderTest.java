package com.octopus.helper;

import com.octopus.testsupport.BaseOctopusServerEnabledTest;
import hudson.plugins.octopusdeploy.AbstractOctopusDeployRecorderPostBuildStep;
import hudson.plugins.octopusdeploy.OctopusDeployServer;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BaseRecorderTest extends BaseOctopusServerEnabledTest {

    public static final String JENKINS_OCTOPUS_SERVER_ID = "default";
    private static MockedStatic<AbstractOctopusDeployRecorderPostBuildStep> postBuildStepMockedStatic;
    private static MockedStatic<Jenkins> jenkinsMockedStatic;
    @TempDir
    private static File tempFile;

    public SpaceScopedClient spaceScopedClient;

    @BeforeAll
    public static void setUpMocks() {
        postBuildStepMockedStatic = Mockito.mockStatic(AbstractOctopusDeployRecorderPostBuildStep.class);
        postBuildStepMockedStatic
                .when(() ->
                        AbstractOctopusDeployRecorderPostBuildStep.getOctopusDeployServer(JENKINS_OCTOPUS_SERVER_ID))
                .thenReturn(new OctopusDeployServer(JENKINS_OCTOPUS_SERVER_ID,
                        server.getOctopusUrl(), server.getApiKey(), true));

        final Jenkins jenkins = mock(Jenkins.class);
        jenkinsMockedStatic = Mockito.mockStatic(Jenkins.class);

        jenkinsMockedStatic.when(Jenkins::get).thenReturn(jenkins);
        when(jenkins.getRootDir()).thenReturn(tempFile);
        when(Jenkins.getInstanceOrNull()).thenReturn(jenkins);
    }

    @BeforeEach
    public void setUp(final TestInfo testInfo) {
        spaceScopedClient = TestHelper.buildSpaceScopedClientForTesting(httpClient,
                server,
                TestHelper.generateSpaceName(testInfo.getDisplayName()));
    }

    @AfterEach
    public void cleanUp() {
        TestHelper.deleteTestingSpace(spaceScopedClient);
    }

    @AfterAll
    public static void cleanUpMocks() {
        postBuildStepMockedStatic.close();
        jenkinsMockedStatic.close();
    }
}
