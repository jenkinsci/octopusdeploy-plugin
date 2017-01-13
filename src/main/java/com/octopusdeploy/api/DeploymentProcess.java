package com.octopusdeploy.api;

import java.util.Set;

/**
 * Deployment process is an Octopus concept that ties a project to the project's individual steps.
 */
public class DeploymentProcess {
    private final String id;
    public String getId() {
        return id;
    }

    private final String projectId;
    public String getProjectId() {
        return projectId;
    }
    
    private final Set<DeploymentProcessStep> steps;
    public Set<DeploymentProcessStep> getSteps() {
        return steps;
    }
    
    public DeploymentProcess(String id, String projectId, Set<DeploymentProcessStep> steps) {
        this.id = id;
        this.projectId = projectId;
        this.steps = steps;
    }

    @Override
    public String toString() {
        return "DeploymentProcess [id=" + id + ", projectId=" + projectId + ", steps=" + steps + "]";
    }
}
