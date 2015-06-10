package com.octopusdeploy.api;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * A data object representing a package override.
 * @author cwetherby
 */
// TODO: What does this need?
public class PackageOverride extends AbstractDescribableImpl<PackageOverride> {
    /**
     * The name of the package specified in this override.
     */
    private String packageName;
    public String getPackageName() {
        return packageName;
    }
    
    /**
     * The version of the package to use instead of default.
     */
    private String version;
    public String getVersion() {
        return version;
    }
    
    @DataBoundConstructor
    public PackageOverride(String packageName, String version) {
        this.packageName = packageName;
        this.version = version;
    }
    
    @Extension
    public static class DescriptorImpl extends Descriptor<PackageOverride> {
        @Override
        public String getDisplayName() {
            return "OctopusDeploy Package Override";
        }
    }
}
