package com.octopusdeploy.api;

import java.util.Set;

/**
 * A Project's release deployment process step.
 */
public class DeploymentProcessStep {
    private final String id;
    public String getId() {
        return id;
    }

    private final String name;
    public String getName() {
        return name;
    }
    
    private final Set<DeploymentProcessStepAction> actions;
    public Set<DeploymentProcessStepAction> getActions() {
        return actions;
    }
    
    public DeploymentProcessStep(String id, String name, Set<DeploymentProcessStepAction> actions) {
        this.id = id;
        this.name = name;
        this.actions = actions;
    }

    @Override
    public String toString() {
        return "DeploymentProcessStep [id=" + id + ", name=" + name + ", actions=" + actions + "]";
    }
}
