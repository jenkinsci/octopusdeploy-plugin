package com.octopusdeploy.api.data;

/**
 * Represents a variable that will be passed to a deployment.
 */
public class Variable {
    private final String name;
    public String getName() {
        return name;
    }
    
    private final String value;
    public String getValue() {
        return value;
    }
    
    private final String id;
    public String getId() {
        return id;
    }
    
    private final String description;
    public String getDescription() {
        return description;
    }
    
    public Variable(String id, String name, String value, String description)
    {
        this.id = id;
        this.name = name;
        this.value = value;
        this.description = description;
    }

    @Override
    public String toString() {
        return "Variable [name=" + name + ", value=" + value + ", id=" + id + ", description=" + description + "]";
    }

}
