package hudson.plugins.octopusdeploy;

import hudson.model.BuildBadgeAction;

/**
 *
 * @author badriance
 */
public class BuildInfoSummary implements BuildBadgeAction {
    
    private final OctopusDeployEventType buildResultType;
    private final String urlName;
    
    public BuildInfoSummary(OctopusDeployEventType buildResultType, String urlName) {
        this.buildResultType = buildResultType;
        this.urlName = urlName;
    }

   
    @Override
    public String getIconFileName() {
        return "/plugin/octopusdeploy/images/octopus-o.png";
    }
    
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

    @Override
    public String getDisplayName() {
        return "OctopusDeploy - " + buildResultType;
    }

    @Override
    public String getUrlName() {
        return urlName;
    }
    
    public enum OctopusDeployEventType {
        Deployment, Release
    }
}
 