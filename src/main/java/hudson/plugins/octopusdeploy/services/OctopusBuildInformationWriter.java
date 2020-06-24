package hudson.plugins.octopusdeploy.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import hudson.FilePath;
import hudson.plugins.octopusdeploy.Log;
import hudson.plugins.octopusdeploy.OctopusBuildInformation;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class OctopusBuildInformationWriter {

    private final Boolean verboseLogging;
    private final Log log;

    public OctopusBuildInformationWriter(Log log, Boolean verboseLogging) {
        this.log = log;
        this.verboseLogging = verboseLogging;
    }

    public void writeToFile(FilePath workspace, final OctopusBuildInformation octopusBuildInformation, final String buildInformationFile) throws IOException, InterruptedException {
        try {
            final Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create();
            if (verboseLogging) {
                log.info("Serializing Octopus build information");
            }

            final String jsonData = gson.toJson(octopusBuildInformation);
            if (verboseLogging) {
                log.info("Serialized Octopus build information - " + jsonData);
            }

            FilePath newBuildInfoFile = new FilePath(workspace, buildInformationFile);
            newBuildInfoFile.write(jsonData, StandardCharsets.UTF_16.toString());

            if (verboseLogging) {
                log.info("Wrote " + buildInformationFile);
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            log.error("Error writing " + buildInformationFile + " file");
            throw e;
        }
    }
}