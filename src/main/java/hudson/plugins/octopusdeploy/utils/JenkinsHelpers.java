package hudson.plugins.octopusdeploy.utils;

import jenkins.model.Jenkins;
import org.jetbrains.annotations.NotNull;

public class JenkinsHelpers {
    @NotNull
    public static Jenkins getJenkins() {
        Jenkins jenkinsInstance = Jenkins.getInstanceOrNull();
        if (jenkinsInstance == null) {
            throw new IllegalStateException("Jenkins instance is null.");
        }
        return jenkinsInstance;
    }
}
