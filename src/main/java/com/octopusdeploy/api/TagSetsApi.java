package com.octopusdeploy.api;

import com.octopusdeploy.api.data.Tag;
import com.octopusdeploy.api.data.TagSet;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import java.io.IOException;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

public class TagSetsApi {
    private final AuthenticatedWebClient webClient;

    public TagSetsApi(AuthenticatedWebClient webClient) { this.webClient = webClient; }

    public Set<TagSet> getAll() throws IllegalArgumentException, IOException {
        TreeSet<TagSet> tagSets = new TreeSet<>(Comparator.comparing(TagSet::getSortOrder).thenComparing(TagSet::getName).thenComparing(TagSet::getId));
        AuthenticatedWebClient.WebResponse response = webClient.get("tagsets/all");
        if(response.isErrorCode()) {
            throw new IOException(String.format("Code %s - %n%s", response.getCode(), response.getContent()));
        }
        JSONArray json = (JSONArray) JSONSerializer.toJSON(response.getContent());
        for(Object obj : json) {
            JSONObject jsonObj = (JSONObject)obj;
            String id = jsonObj.getString("Id");
            String name = jsonObj.getString("Name");
            String description = jsonObj.getString("Description");
            int sortOrder = jsonObj.getInt("SortOrder");
            JSONArray tagsJson = (JSONArray)JSONSerializer.toJSON(jsonObj.getString("Tags"));
            Set<Tag> tags = new TreeSet<>(Comparator.comparing(Tag::getSortOrder).thenComparing(Tag::getName).thenComparing(Tag::getId));
            for(Object tagObj : tagsJson) {
                JSONObject tagJsonObj = (JSONObject)tagObj;
                String tagId = tagJsonObj.getString("Id");
                String tagName = tagJsonObj.getString("Name");
                String tagCanonicalName = tagJsonObj.getString("CanonicalTagName");
                int tagSortOrder = tagJsonObj.getInt("SortOrder");
                tags.add(new Tag(tagId, tagName, tagCanonicalName, tagSortOrder));
            }
            tagSets.add(new TagSet(id, name, description, sortOrder, tags));
        }

        return tagSets;
    }
}
