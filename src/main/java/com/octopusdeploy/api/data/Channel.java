package com.octopusdeploy.api.data;

/**
 * Simple representation of a Channel in Octopus.
 */
public class Channel {
    private final String id;
    public String getId() {
        return id;
    }
    
    private final String name;
    public String getName() {
        return name;
    }
    
    private final String description;
    public String getDescription() {
        return description;
    }
    
    private final String projectId;
    public final String getProjectId() {
        return projectId;
    }
    
    private final boolean isDefault;
    public boolean getIsDefault() {
        return isDefault;
    }
    
    public Channel(String id, String name, String description, String projectId, boolean isDefault) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.projectId = projectId;
        this.isDefault = isDefault;
    }
    
    @Override
    public String toString() {
        return String.format("id= %s, name= %s, description= %s, projectId= %s, isDefault= %b", id, name, description, projectId, isDefault);
    }
}
