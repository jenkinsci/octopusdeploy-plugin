package com.octopusdeploy.api.data;

public class Space {
    private final String name;
    public String getName() { return name; }

    private final String id;
    public String getId() { return id; }

    public Space(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() { return "Space [name=" + name + ", id=" + id + "]"; }
}
