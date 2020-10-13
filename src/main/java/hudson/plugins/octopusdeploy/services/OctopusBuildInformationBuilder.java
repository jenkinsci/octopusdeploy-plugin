package hudson.plugins.octopusdeploy.services;

import hudson.plugins.octopusdeploy.Commit;
import hudson.plugins.octopusdeploy.OctopusBuildInformation;

import java.util.List;

public class OctopusBuildInformationBuilder {

    public OctopusBuildInformation build(
            final String vcsType,
            final String vcsRoot,
            final String vcsCommitNumber,
            final List<Commit> commits,
            final String commentParser,
            final String buildUrl,
            final String buildNumber,
            final String branch) {

        final OctopusBuildInformation buildInformation = new OctopusBuildInformation();

        buildInformation.Commits = commits;
        buildInformation.CommentParser = commentParser;
        buildInformation.BuildNumber = buildNumber;
        buildInformation.BuildUrl = buildUrl;
        buildInformation.VcsType = vcsType;
        buildInformation.VcsRoot = vcsRoot;
        buildInformation.VcsCommitNumber = vcsCommitNumber;
        buildInformation.Branch = branch;

        return buildInformation;
    }
}
