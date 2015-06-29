package hudson.plugins.octopusdeploy;

import hudson.model.BuildBadgeAction;

/**
 * Plugin portion to show information on the Build Summary page, and on the line entries for build history.
 */
public class BuildInfoSummary implements BuildBadgeAction {
    
    private final OctopusDeployEventType buildResultType;
    private final String urlName;
    
    public BuildInfoSummary(OctopusDeployEventType buildResultType, String urlName) {
        this.buildResultType = buildResultType;
        this.urlName = urlName;
    }
   
    /**
     * The default file for the image used by this summary entry.
     * @return relative path to a file.
     */
    @Override
    public String getIconFileName() {
        return "/plugin/octopusdeploy/images/octopus-o.png";
    }
    
    /**
     * Get an icon that is differentiated depending on which kind of
     * action this is representing.
     * @return icon file path.
     */
    public String getLabelledIconFileName() {
        String filename;
        switch (buildResultType) {
            case Deployment:
                filename = "octopus-d.png";
                break;
            case Release:
                filename = "octopus-r.png";
                break;
            default:
                filename = "octopus-o.png";
                break;
        }
        return "/plugin/octopusdeploy/images/" + filename;
    }

    /**
     * Display name for this summary entry.
     * @return OctopusDeploy - [the type of action this represents]
     */
    @Override
    public String getDisplayName() {
        return "OctopusDeploy - " + buildResultType;
    }

    /**
     * The URL to use in this summary entry.
     * @return URL to link to.
     */
    @Override
    public String getUrlName() {
        return urlName;
    }
    
    /**
     * The types of OctopusDeploy even that this class can represent.
     */
    public enum OctopusDeployEventType {
        Deployment, Release
    }
}
 