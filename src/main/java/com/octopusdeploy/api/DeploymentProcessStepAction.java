package com.octopusdeploy.api;

import java.util.Map;

/**
* Actions for Deployment Process Steps.
*/
public class DeploymentProcessStepAction {  
    private final String id;
    public String getId() {
        return id;
    }

    private final String name;
    public String getName() {
        return name;
    }

    private final String actionType;
    public String getActionType() {
        return actionType;
    }

    private final Map<String, String> properties;
    public Map<String, String> getProperties() {
        return properties;
    }

    public DeploymentProcessStepAction(String id, String name, String actionType, Map<String, String> properties)
    {
        this.id = id;
        this.name = name;
        this.actionType = actionType;
        this.properties = properties;
    }
}
