package com.octopusdeploy.api.data;

/**
 * A simplified representation of a Release in OctopusDeploy.
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
    
    private final String channelId;
    public String getChannelId() {
        return channelId;
    }
    
    private final String releaseNotes;
    public String getReleaseNotes() {
        return releaseNotes;
    }
        
    private final String version;
    public String getVersion() {
        return version;
    }
    
    public Release(String id, String projectId, String channelId, String releaseNotes, String version){
        this.id = id;
        this.projectId = projectId;
        this.channelId = channelId;
        this.releaseNotes = releaseNotes;
        this.version = version;
    }

    @Override
    public String toString() {
        return "Release [id=" + id + ", projectId=" + projectId + ", ChannelId=" + channelId + ", releaseNotes=" + releaseNotes + ", version=" + version + "]";
    }
}
