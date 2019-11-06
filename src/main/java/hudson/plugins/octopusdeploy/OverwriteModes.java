package hudson.plugins.octopusdeploy;

import java.util.LinkedHashMap;
import java.util.Map;

public class OverwriteModes {
    public static Map<String, String> getOverwriteModes() {
        Map<String, String> overwriteModes = new LinkedHashMap<>();
        overwriteModes.put(OverwriteMode.FailIfExists.name(), "Fail if exists");
        overwriteModes.put(OverwriteMode.OverwriteExisting.name(), "Overwrite existing");
        overwriteModes.put(OverwriteMode.IgnoreIfExists.name(), "Ignore if exists");
        return overwriteModes;
    }
}
