package com.octopusdeploy.api;

/**
 * Represents a Tenant.
 */
public class Tenant {
    private final String name;
    public String getName() {
        return name;
    }

    private final String id;
    public String getId() {
        return id;
    }

    public Tenant(String id, String name)
    {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return "Tenant [name=" + name + ", id=" + id + "]";
    }
}
