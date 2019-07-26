package com.octopusdeploy.api.data;

import java.util.Set;

public class TagSet {
    private final String name;
    public String getName() { return name; }

    private final String id;
    public String getId() { return id; }

    private final String description;
    public String getDescription() { return description; }

    private final int sortOrder;
    public int getSortOrder() { return sortOrder; }

    private final Set<Tag> tags;
    public Set<Tag> getTags() { return tags; }

    public TagSet(String id, String name, String description, int sortOrder, Set<Tag> tags) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
        this.tags = tags;
    }

    @Override
    public String toString() {
        return "TagSet [name=" + name + ", id=" + id + ", description=" + description + ", sortOrder=" + sortOrder + ", tagCount=" + tags.size() + "]";
    }
}
