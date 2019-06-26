package hudson.plugins.octopusdeploy;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Descriptor;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.*;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.List;

public class OctoInstallation extends ToolInstallation implements NodeSpecific<OctoInstallation>, EnvironmentSpecific<OctoInstallation> {

    public static transient final String DEFAULT = "Default";

    private static final long serialVersionUID = 1;

    @SuppressWarnings("unused")
    /**
     * Backward compatibility
     */
    private transient String pathToOcto;

    @DataBoundConstructor
    public OctoInstallation(String name, String home) {
        super(name, home, null);
    }

    @Override
    public OctoInstallation forNode(@NonNull Node node, TaskListener log) throws IOException, InterruptedException {
        return new OctoInstallation(getName(), translateFor(node, log));
    }
    @Override
    public OctoInstallation forEnvironment(EnvVars environment) {
        return new OctoInstallation(getName(), environment.expand(getHome()));
    }

    protected Object readResolve() {
        if (this.pathToOcto != null) {
            return new OctoInstallation(this.getName(), this.pathToOcto);
        }
        return this;
    }

    public String getPathToOctoExe() {
        return getHome();
    }

    public static OctoInstallation getDefaultInstallation() {
        DescriptorImpl octoTools = Jenkins.getInstance().getDescriptorByType(OctoInstallation.DescriptorImpl.class);
        OctoInstallation tool = octoTools.getInstallation(OctoInstallation.DEFAULT);
        if (tool != null) {
            return tool;
        } else {
            OctoInstallation[] installations = octoTools.getInstallations();
            if (installations.length > 0) {
                return installations[0];
            } else {
                onLoaded();
                return octoTools.getInstallations()[0];
            }
        }
    }

    @Initializer(after = InitMilestone.EXTENSIONS_AUGMENTED)
    public static void onLoaded() {
        DescriptorImpl descriptor = (OctoInstallation.DescriptorImpl) Jenkins.getInstance().getDescriptor(OctoInstallation.class);
        assert descriptor != null;
        OctoInstallation[] installations = descriptor.getInstallations();
        if (installations != null && installations.length > 0) {
            return;
        }
        String defaultOctoExe = isWindows() ? "Octo.exe" : "Octo";
        OctoInstallation tool = new OctoInstallation(DEFAULT, defaultOctoExe);
        descriptor.setInstallations(tool);
        descriptor.save();
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    private static boolean isWindows() {
        return File.pathSeparatorChar == ';';
    }


    @Extension
    public static class DescriptorImpl extends ToolDescriptor<OctoInstallation> {

        public DescriptorImpl() {
            super();
            load();
        }

        public String getDisplayName() {
            return "Octopus CLI";
        }

        @Nullable
        public OctoInstallation getInstallation(String name) {
            for (OctoInstallation i : getInstallations()) {
                if (i.getName().equals(name)) {
                    return i;
                }
            }
            return null;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) {
            JSONObject json = formData.getJSONObject("octopusCli");

            if(!json.isEmpty()) {
                OctoInstallation[] tools = req.bindJSONToList(OctoInstallation.class, json.get("tools"))
                        .toArray((OctoInstallation[]) Array.newInstance(OctoInstallation.class, 0));
                setInstallations(tools);
            }
            save();

            return true;
        }
    }
}
