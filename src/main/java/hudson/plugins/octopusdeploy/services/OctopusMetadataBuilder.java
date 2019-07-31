package hudson.plugins.octopusdeploy.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import hudson.plugins.octopusdeploy.Commit;
import hudson.plugins.octopusdeploy.OctopusPackageMetadata;

import java.util.List;

public class OctopusMetadataBuilder {

    public OctopusPackageMetadata build(
            final String vcsType,
            final String vcsRoot,
            final String vcsCommitNumber,
            final List<Commit> commits,
            final String commentParser,
            final String buildUrl,
            final String buildNumber) {

        final OctopusPackageMetadata metadata = new OctopusPackageMetadata();

        metadata.Commits = commits;
        metadata.CommentParser = commentParser;
        metadata.BuildNumber = buildNumber;
        metadata.BuildUrl = buildUrl;
        metadata.VcsType = vcsType;
        metadata.VcsRoot = vcsRoot;
        metadata.VcsCommitNumber = vcsCommitNumber;

        return metadata;
    }
}
