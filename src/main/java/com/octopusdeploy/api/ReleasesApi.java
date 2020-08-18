package com.octopusdeploy.api;

import com.octopusdeploy.api.data.Release;
import com.octopusdeploy.api.data.SelectedPackage;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

public class ReleasesApi {
    private final static String UTF8 = "UTF-8";
    private final AuthenticatedWebClient webClient;

    public ReleasesApi(AuthenticatedWebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Creates a release in octopus deploy.
     * @param project The project id
     * @param releaseVersion The version number for this release.
     * @return content from the API post
     * @throws java.io.IOException When the AuthenticatedWebClient receives and error response code
     */
    public String createRelease(String project, String releaseVersion) throws IOException {
        return createRelease(project, releaseVersion, null);
    }

    /**
     * Creates a release in octopus deploy.
     * @param project The project id.
     * @param releaseVersion The version number for this release.
     * @param releaseNotes Release notes to be associated with this release.
     * @return content from the API post
     * @throws java.io.IOException When the AuthenticatedWebClient receives and error response code
     */
    public String createRelease(String project, String releaseVersion, String releaseNotes) throws IOException {
        return createRelease(project, releaseVersion, null, releaseNotes, null);
    }

    /**
     * Creates a release in octopus deploy.
     * @param project The project id
     * @param releaseVersion The version number for this release.
     * @param channelId The channel to create the release on.
     * @param releaseNotes Release notes to be associated with this release.
     * @param selectedPackages Packages to be deployed with this release.
     * @return content from the API post
     * @throws java.io.IOException When the AuthenticatedWebClient receives and error response code
     */
    public String createRelease(String project, String releaseVersion, String channelId, String releaseNotes, Set<SelectedPackage> selectedPackages) throws IOException {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append(String.format("{ProjectId:\"%s\",Version:\"%s\"", project, releaseVersion));
        if (channelId != null && !channelId.isEmpty()) {
            jsonBuilder.append(String.format(",ChannelId:\"%s\"", channelId));
        }
        if (releaseNotes != null && !releaseNotes.isEmpty()) {
            jsonBuilder.append(String.format(",ReleaseNotes:\"%s\"", releaseNotes));
        }
        if (selectedPackages != null && !selectedPackages.isEmpty()) {
            jsonBuilder.append(",SelectedPackages:[");
            Set<String> selectedPackageStrings = new HashSet<String>();
            for (SelectedPackage selectedPackage : selectedPackages) {
                // StepName has been deprecated, ActionName should now be used. Continue passing StepName in case an older
                // version of Octopus server is in use.
                String actionName = selectedPackage.getStepName();
                selectedPackageStrings.add(String.format("{StepName:\"%s\",ActionName:\"%s\",PackageReferenceName:\"%s\",Version:\"%s\"}", actionName, actionName, selectedPackage.getPackageReferenceName(), selectedPackage.getVersion()));
            }
            jsonBuilder.append(StringUtils.join(selectedPackageStrings, ","));
            jsonBuilder.append("]");
        }
        jsonBuilder.append("}");
        String json = jsonBuilder.toString();
        byte[] data = json.getBytes(Charset.forName(UTF8));
        AuthenticatedWebClient.WebResponse response = webClient.post("releases", data);
        if (response.isErrorCode()) {
            String errorMsg = ErrorParser.getErrorsFromResponse(response.getContent());
            throw new IOException(String.format("Code %s - %n%s", response.getCode(), errorMsg));
        }
        return response.getContent();
    }

    /**
     * Get all releases for a given project from the Octopus server;
     * @param projectId the id of the project to get the releases for
     * @return A set of all releases for a given project
     * @throws IllegalArgumentException when the web client receives a bad parameter
     * @throws IOException When the AuthenticatedWebClient receives and error response code
     */
    public Set<Release> getReleasesForProject(String projectId) throws IllegalArgumentException, IOException {
        HashSet<Release> releases = new HashSet<Release>();
        AuthenticatedWebClient.WebResponse response = webClient.get("projects/" + projectId + "/releases");
        if (response.isErrorCode()) {
            throw new IOException(String.format("Code %s - %n%s", response.getCode(), response.getContent()));
        }
        JSONObject json = (JSONObject)JSONSerializer.toJSON(response.getContent());
        for (Object obj : json.getJSONArray("Items")) {
            JSONObject jsonObj = (JSONObject)obj;
            String id = jsonObj.getString("Id");
            String version = jsonObj.getString("Version");
            String channelId = jsonObj.getString("ChannelId");
            String ReleaseNotes = jsonObj.getString("ReleaseNotes");
            releases.add(new Release(id, projectId, channelId, ReleaseNotes, version));
        }
        return releases;
    }

    /**
     * Get the partial Octopus portal URL for a given release version of a project;
     * @param projectId the id of the project to get the releases for
     * @param releaseVersion the version of the release to get
     * @return A version of releases for a given project
     * @throws IllegalArgumentException when the web client receives a bad parameter
     * @throws IOException When the AuthenticatedWebClient receives and error response code
     */
    public String getPortalUrlForRelease(String projectId, String releaseVersion) throws IllegalArgumentException, IOException {
        AuthenticatedWebClient.WebResponse response = webClient.get("projects/" + projectId + "/releases/" + releaseVersion);
        if (response.isErrorCode()) {
            throw new IOException(String.format("Code %s - %n%s", response.getCode(), response.getContent()));
        }
        JSONObject json = (JSONObject)JSONSerializer.toJSON(response.getContent());
        JSONObject links = json.getJSONObject("Links");
        return links.getString("Web");
    }

    /**
     * Get the partial Octopus portal URL for the latest release of a project
     * @param projectId the id of the project to get the releases for
     * @return A version of releases for a given project
     * @throws IllegalArgumentException when the web client receives a bad parameter
     * @throws IOException When the AuthenticatedWebClient receives and error response code
     */
    public String getPortalUrlForLatestRelease(String projectId) throws IllegalArgumentException, IOException {
        AuthenticatedWebClient.WebResponse response = webClient.get("projects/" + projectId + "/releases");
        if (response.isErrorCode()) {
            throw new IOException(String.format("Code %s - %n%s", response.getCode(), response.getContent()));
        }
        JSONObject json = (JSONObject)JSONSerializer.toJSON(response.getContent());
        JSONArray items = json.getJSONArray("Items");
        if (items.size() != 0) {
            return items
                    .getJSONObject(0)
                    .getJSONObject("Links")
                    .getString("Web");
        }

        return null;
    }
}
