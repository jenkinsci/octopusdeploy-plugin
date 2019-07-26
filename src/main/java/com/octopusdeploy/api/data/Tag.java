package com.octopusdeploy.api.data;

public class Tag {
    private final String name;
    public String getName() { return name; }

    private final String id;
    public String getId() { return id; }

    private final int sortOrder;
    public int getSortOrder() { return sortOrder; }

    private final String canonicalName;
    public String getCanonicalName() { return canonicalName; }

    public Tag(String id, String name, String canonicalName, int sortOrder) {
        this.id = id;
        this.name = name;
        this.canonicalName = canonicalName;
        this.sortOrder = sortOrder;
    }

    @Override
    public String toString() {
        return "Tag [name=" + name + ", canonicalName=" + canonicalName + ", id=" + id + ", sortOrder=" + sortOrder + "]";
    }
}
