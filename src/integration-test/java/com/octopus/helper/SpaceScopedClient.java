package com.octopus.helper;

import com.octopus.sdk.Repository;
import com.octopus.sdk.domain.Channel;
import com.octopus.sdk.domain.Environment;
import com.octopus.sdk.domain.Lifecycle;
import com.octopus.sdk.domain.Project;
import com.octopus.sdk.domain.ProjectGroup;
import com.octopus.sdk.domain.Space;
import com.octopus.sdk.domain.TagSet;
import com.octopus.sdk.domain.Tenant;
import com.octopus.sdk.http.OctopusClient;
import com.octopus.sdk.model.channel.ChannelResource;
import com.octopus.sdk.model.environment.EnvironmentResourceWithLinks;
import com.octopus.sdk.model.lifecycle.LifecycleResource;
import com.octopus.sdk.model.project.ProjectResource;
import com.octopus.sdk.model.projectgroup.ProjectGroupResource;
import com.octopus.sdk.model.space.SpaceHome;
import com.octopus.sdk.model.tag.TagResource;
import com.octopus.sdk.model.tagset.TagSetResource;
import com.octopus.sdk.model.tagset.TagSetResourceWithLinks;
import com.octopus.sdk.model.tenant.TenantResource;

import java.io.IOException;
import java.util.Objects;

public class SpaceScopedClient {
    private final OctopusClient client;
    private final Repository repository;
    private final Space space;
    private final SpaceHome spaceHome;

    public SpaceScopedClient(final OctopusClient client,
                             final Repository repository,
                             final Space space,
                             final SpaceHome spaceHome) {
        this.client = client;
        this.repository = repository;
        this.space = space;
        this.spaceHome = spaceHome;
    }

    @SuppressWarnings("unused")
    public OctopusClient getClient() {
        return client;
    }

    @SuppressWarnings("unused")
    public SpaceHome getSpaceHome() {
        return spaceHome;
    }

    public Repository getRepository() {
        return repository;
    }

    public Space getSpace() {
        return space;
    }

    public String getSpaceId() {
        return space.getProperties().getId();
    }

    @SuppressWarnings("UnusedReturnValue")
    public Environment createEnvironment(final String environmentName) {
        try {
            return space.environments().create(new EnvironmentResourceWithLinks(environmentName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ProjectGroup createProjectGroup(final String projectGroupName) {
        try {
            return space.projectGroups().create(new ProjectGroupResource(projectGroupName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Project createProject(String projectName, String existingProjectGroupId) {
        try {
            Lifecycle lifecycle;
            if (space.lifecycles().getByName("LifeCycle1").isPresent()) {
                lifecycle = space.lifecycles().getByName("LifeCycle1").get();
            } else {
                lifecycle = space.lifecycles().create(new LifecycleResource("LifeCycle1"));
            }
            return space.projects().create(new ProjectResource(projectName, lifecycle.getProperties().getId(), existingProjectGroupId));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("UnusedReturnValue")
    public Channel createChannel(final String channelName, final String existingProjectId) {
        try {
            return space.channels().create(new ChannelResource(channelName, existingProjectId));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    @SuppressWarnings("UnusedReturnValue")
    public Tenant createTenant(final String tenantName) {
        try {
            return space.tenants().create(new TenantResource(tenantName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("UnusedReturnValue")
    public TagSet createTagSet(final String tagSetName) {
        try {
            return space.tagSets().create(new TagSetResource(tagSetName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("UnusedReturnValue")
    public TagSet createTagInTagSet(final String tagName, final String tagColorHex, final String tagSetName) {
        try {
            final TagSetResourceWithLinks tagSetResourceWithLinks =
                    Objects.requireNonNull(space.tagSets().getByName(tagSetName).orElse(null)).getProperties();
            tagSetResourceWithLinks.addTagsItem(new TagResource(tagName, tagColorHex));
            return space.tagSets().update(tagSetResourceWithLinks);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
