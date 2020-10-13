package hudson.plugins.octopusdeploy;

import java.util.ArrayList;
import java.util.List;

public class OctopusBuildInformation {
    public String BuildEnvironment;
    public String Branch;
    public String CommentParser;
    public String BuildNumber;
    public String BuildUrl;
    public String VcsType;
    public String VcsRoot;
    public String VcsCommitNumber;

    public List<Commit> Commits;

    public OctopusBuildInformation() {
        BuildEnvironment = "Jenkins";
        Commits = new ArrayList<>();
    }
}
