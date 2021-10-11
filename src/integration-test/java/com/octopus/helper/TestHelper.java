package com.octopus.helper;

import com.google.common.collect.Sets;
import com.octopus.sdk.Repository;
import com.octopus.sdk.api.SpaceHomeApi;
import com.octopus.sdk.api.UserApi;
import com.octopus.sdk.domain.Space;
import com.octopus.sdk.http.OctopusClient;
import com.octopus.sdk.model.space.SpaceHome;
import com.octopus.sdk.model.space.SpaceOverviewResource;
import com.octopus.sdk.model.space.SpaceOverviewWithLinks;
import com.octopus.testsupport.OctopusDeployServer;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.net.URL;
import java.util.Set;

public class TestHelper {

    private static final int MAX_SPACE_NAME_LENGTH = 20;

    public static String generateSpaceName(final String testName) {
        final String withoutBraces = testName.substring(0, testName.length() - 2);
        final int startIndex = Math.min(withoutBraces.length(), MAX_SPACE_NAME_LENGTH);

        return withoutBraces.substring(withoutBraces.length() - startIndex);
    }

    public static SpaceScopedClient buildSpaceScopedClientForTesting(final OkHttpClient httpClient,
                                                                     final OctopusDeployServer server,
                                                                     final String spaceName) {
        try {
            final OctopusClient client = new OctopusClient(httpClient, new URL(server.getOctopusUrl()), server.getApiKey());
            final Set<String> spaceManagers = Sets.newHashSet(UserApi.create(client).getCurrentUser().getId());
            final Repository repository = new Repository(client);

            final Space space = repository.spaces().create(new SpaceOverviewWithLinks(spaceName, spaceManagers));
            final SpaceHome spaceHome = new SpaceHomeApi(client).getBySpaceOverview(space.getProperties());

            return new SpaceScopedClient(client, repository, space, spaceHome);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void deleteTestingSpace(final SpaceScopedClient spaceScopedClient) {
        final SpaceOverviewResource resource = spaceScopedClient.getSpace().getProperties();
        resource.setTaskQueueStopped(true);
        try {
            spaceScopedClient.getRepository().spaces().update(resource);
            spaceScopedClient.getRepository().spaces().delete(resource);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
