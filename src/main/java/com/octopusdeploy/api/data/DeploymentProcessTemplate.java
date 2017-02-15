package com.octopusdeploy.api.data;

import java.util.Set;

/**
 * Represents a set of packages from the deployment process template
 */
public class DeploymentProcessTemplate {
    private final String id;
    public String getId() {
        return id;
    }

    private final String projectId;
    public String getProjectId() {
        return projectId;
    }
    
    private final Set<SelectedPackage> packages;
    public Set<SelectedPackage> getSteps() {
        return packages;
    }
    
    public DeploymentProcessTemplate(String id, String projectId, Set<SelectedPackage> packages) {
        this.id = id;
        this.projectId = projectId;
        this.packages = packages;
    }

    @Override
    public String toString() {
        return "DeploymentProcessTemplate [id=" + id + ", projectId=" + projectId + ", packages=" + packages + "]";
    }
}
