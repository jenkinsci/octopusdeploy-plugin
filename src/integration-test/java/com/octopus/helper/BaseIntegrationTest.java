package com.octopus.helper;

import com.google.common.collect.Sets;
import com.octopus.sdk.Repository;
import com.octopus.sdk.api.SpaceOverviewApi;
import com.octopus.sdk.api.UserApi;
import com.octopus.sdk.domain.Lifecycle;
import com.octopus.sdk.domain.Project;
import com.octopus.sdk.domain.ProjectGroup;
import com.octopus.sdk.domain.Space;
import com.octopus.sdk.domain.TagSet;
import com.octopus.sdk.http.OctopusClient;
import com.octopus.sdk.model.channel.ChannelResource;
import com.octopus.sdk.model.environment.EnvironmentResource;
import com.octopus.sdk.model.lifecycle.LifecycleResource;
import com.octopus.sdk.model.project.ProjectResource;
import com.octopus.sdk.model.projectgroup.ProjectGroupResource;
import com.octopus.sdk.model.release.ReleaseResource;
import com.octopus.sdk.model.space.SpaceOverviewWithLinks;
import com.octopus.sdk.model.tag.TagResource;
import com.octopus.sdk.model.tagset.TagSetResource;
import com.octopus.sdk.model.tagset.TagSetResourceWithLinks;
import com.octopus.sdk.model.tenant.TenantResource;
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
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BaseIntegrationTest extends BaseOctopusServerEnabledTest {

    public static final String JENKINS_OCTOPUS_SERVER_ID = "default";
    private static final int MAX_SPACE_NAME_LENGTH = 20;

    @TempDir
    private static File tempFile;

    private OctopusClient client;

    public static MockedStatic<AbstractOctopusDeployRecorderPostBuildStep> postBuildStepMockedStatic;
    public static MockedStatic<Jenkins> jenkinsMockedStatic;
    public Space space;

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
    public void setUp(final TestInfo testInfo) throws IOException {
        client = new OctopusClient(httpClient, new URL(server.getOctopusUrl()), server.getApiKey());
        final Set<String> spaceManagers =
                Sets.newHashSet(UserApi.create(client).getCurrentUser().getProperties().getId());
        final Repository repository = new Repository(client);
        space = repository
                .spaces()
                .create(new SpaceOverviewWithLinks(generateSpaceName(testInfo.getDisplayName()), spaceManagers));
        initTestEnvironment();
    }

    @AfterEach
    public void cleanUp() throws IOException {
        deleteSpaceValidly(SpaceOverviewApi.create(client), space.getProperties());
    }

    @AfterAll
    public static void cleanUpMocks() {
        postBuildStepMockedStatic.close();
        jenkinsMockedStatic.close();
    }

    private String generateSpaceName(final String testName) {
        final String withoutBraces = testName.substring(0, testName.length() - 2);
        final int startIndex = Math.min(withoutBraces.length(), MAX_SPACE_NAME_LENGTH);

        return withoutBraces.substring(withoutBraces.length() - startIndex);
    }

    private void initTestEnvironment() throws IOException {
        final ProjectGroup projectGroup = space.projectGroups().create(new ProjectGroupResource("ProjectGroup1"));
        final Lifecycle lifecycle = space.lifecycles().create(new LifecycleResource("LifeCycle1"));

        final Project project1 = space.projects().create(new ProjectResource("Project1",
                lifecycle.getProperties().getId(),
                projectGroup.getProperties().getId()));
        for (String name : Arrays.asList("Project2", "Project3")) {
            space.projects().create(new ProjectResource(name,
                    lifecycle.getProperties().getId(),
                    projectGroup.getProperties().getId()));
        }

        for (String name : Arrays.asList("Channel1", "Channel2", "Channel3")) {
            space.channels().create(new ChannelResource(name, project1.getProperties().getId()));
        }

        space.releases().create(new ReleaseResource("1.0.0", project1.getProperties().getId()));

        final TagSetResourceWithLinks tagSetWithLinks =
                space.tagSets().create(new TagSetResource("TagSet1")).getProperties();
        tagSetWithLinks.addTagsItem(new TagResource("Tag1", "#111111"));
        tagSetWithLinks.addTagsItem(new TagResource("Tag2", "#222222"));
        final TagSet updatedTagSet = space.tagSets().update(tagSetWithLinks);

        final TenantResource tenant1 = new TenantResource("Tenant1");
        tenant1.setTenantTags(new HashSet<>(updatedTagSet.getProperties().getTags()
                .stream()
                .map(TagResource::getCanonicalTagName)
                .collect(Collectors.toList())));
        space.tenants().create(tenant1);
        for (String name : Arrays.asList("Tenant2", "Tenant3")) {
            space.tenants().create(new TenantResource(name));
        }

        for (String name : Arrays.asList("Environment1", "Environment2", "Environment3")) {
            space.environments().create(new EnvironmentResource(name));
        }
    }
}
