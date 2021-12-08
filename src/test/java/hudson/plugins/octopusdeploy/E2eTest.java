package hudson.plugins.octopusdeploy;

import com.octopus.sdk.Repository;
import com.octopus.sdk.domain.BuildInformation;
import com.octopus.sdk.domain.Space;
import com.octopus.sdk.http.OctopusClient;
import com.octopus.testsupport.OctopusDeployServerFactory;
import hudson.model.FreeStyleProject;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SingleFileSCM;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/*
E2E Tests run under JUnit-4 and test must be located in the
default Maven test path due to Jenkins test harness limitations.
 */
public class E2eTest {

    private final com.octopus.testsupport.OctopusDeployServer server = OctopusDeployServerFactory.create();
    private Space space;

    @Rule
    public final JenkinsRule jenkinsRule = new JenkinsRule();

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        // Configure Jenkins Octopus Server connection
        final OctopusDeployServer testServer = new OctopusDeployServer("TestServer",
                server.getOctopusUrl(), server.getApiKey(), true);
        final OctopusDeployPlugin.DescriptorImpl configDescriptor
                = jenkinsRule.jenkins.getDescriptorByType(OctopusDeployPlugin.DescriptorImpl.class);
        configDescriptor.setOctopusDeployServers(Collections.singletonList(testServer));
        configDescriptor.save();

        // Configure Jenkins OctoCLI
        final OctoInstallation installation =
                new OctoInstallation("Default", System.getenv("OCTOPUS_CLI_PATH"));
        final OctoInstallation.DescriptorImpl cliDescriptor =
                jenkinsRule.jenkins.getDescriptorByType(OctoInstallation.DescriptorImpl.class);
        cliDescriptor.setInstallations(installation);
        cliDescriptor.save();

        // Get Space and configure Octopus server for tests
        final OctopusClient client =
                new OctopusClient(new OkHttpClient(), new URL(server.getOctopusUrl()), server.getApiKey());
        space = new Repository(client).spaces().getAll().get(0);
    }

    @Test
    public void e2eBuildStepTest() throws Exception {
        final FreeStyleProject project = (FreeStyleProject) jenkinsRule.jenkins
                .createProjectFromXML("E2E Test", new ByteArrayInputStream(getProjectConfiguration().getBytes()));
        project.setCustomWorkspace(temporaryFolder.getRoot().toString());
        project.setScm(new SingleFileSCM("test.txt", "test"));

        jenkinsRule.buildAndAssertSuccess(project);

        // OctopusDeployPackRecorder - ZIP file created on local file system
        assertThat(new File(String.valueOf(temporaryFolder.getRoot().toPath().resolve("PackageId.1.0.0.zip")))).exists().isFile();

        // OctopusDeployPushRecorder - Packaged pushed to Octopus
        assertThat(space.packages().getAll())
                .flatExtracting("title", "version").contains("PackageId", "1.0.0");

        // OctopusDeployPushBuildInformationRecorder - Build info pushed to Octopus
        assertThat(space.buildInformation().getAll().stream().map(BuildInformation::getProperties))
                .flatExtracting("packageId", "version").contains("PackageId", "1.0.0");
    }

    private String getProjectConfiguration() throws URISyntaxException, IOException {
        final String rawConfig = new String(Files.readAllBytes(Paths.get(Objects.requireNonNull(getClass()
                        .getClassLoader()
                        .getResource("e2eTestProjectConfig.xml"))
                .toURI())));

        return rawConfig.replace("<outputPath>.</outputPath>",
                "<outputPath>" + temporaryFolder.getRoot() + "</outputPath>");
    }

}
