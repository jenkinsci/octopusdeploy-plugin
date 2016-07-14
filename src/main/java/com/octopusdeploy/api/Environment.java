package com.octopusdeploy.api;

/**
 * Represents an environment. Environments are user-defined and map to real world
 * deployment environments such as development, staging, test and production. 
 * Projects are deployed to environments.
 */
public class Environment {
    private final String name;
    public String getName() {
        return name;
    }
    
    private final String id;
    public String getId() {
        return id;
    }
    
    private final String description;
    public String getDescription() {
        return description;
    }
    
    public Environment(String id, String name, String description)
    {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    @Override
    public String toString() {
        return "Environment [name=" + name + ", id=" + id + ", description=" + description + "]";
    }
}
