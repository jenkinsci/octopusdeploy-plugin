package com.octopusdeploy.api;

/**
 *
 */
public class Release {
    private final String id;
    public String getId() {
        return id;
    }
    
    private final String projectId;
    public String getProjectId() {
        return projectId;
    }
    
    private final String releaseNotes;
    public String getReleaseNotes() {
        return releaseNotes;
    }
        
    private final String version;
    public String getVersion() {
        return version;
    }
    
    public Release(String id, String projectId, String releaseNotes, String version){
        this.id = id;
        this.projectId = projectId;
        this.releaseNotes = releaseNotes;
        this.version = version;
    }
}
