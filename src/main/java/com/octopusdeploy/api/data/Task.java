
package com.octopusdeploy.api.data;

/**
 * Task.
 * Tasks are what octopus uses when it is doing something.
 */
public class Task {
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
    
    private final String state;
    public String getState() {
        return state;
    }
    
    private final boolean isCompleted;
    public boolean getIsCompleted() {
        return isCompleted;
    }
    
    public Task(String id, String name, String description, String state, boolean isCompleted) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.state = state;
        this.isCompleted = isCompleted;
    }

    @Override
    public String toString() {
        return "Task [id=" + id + ", name=" + name + ", description=" + description + ", state=" + state + ", isCompleted=" + isCompleted + "]";
    }
    
}
