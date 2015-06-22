package hudson.plugins.octopusdeploy;

import hudson.Extension;
import hudson.model.*;
import java.io.Serializable;
import org.kohsuke.stapler.*;
import org.kohsuke.stapler.export.*;

/**
 * A data object representing a package configuration.
 * @author cwetherby
 */
@ExportedBean
public class PackageConfiguration extends AbstractDescribableImpl<PackageConfiguration> implements Serializable {
    /**
     * The name of the package..
     */
    private final String packageName;
    @Exported
    public String getPackageName() {
        return packageName;
    }
    
    /**
     * The version of the package to use.
     */
    private final String packageVersion;
    @Exported
    public String getPackageVersion() {
        return packageVersion;
    }
    
    @DataBoundConstructor
    public PackageConfiguration(String packageName, String packageVersion) {
        this.packageName = packageName;
        this.packageVersion = packageVersion;
    }
    
    @Extension
    public static class DescriptorImpl extends Descriptor<PackageConfiguration> {
        @Override
        public String getDisplayName() {
            return "";
        }
    }
}
