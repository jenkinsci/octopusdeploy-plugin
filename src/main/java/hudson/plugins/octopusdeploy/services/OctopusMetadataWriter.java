package hudson.plugins.octopusdeploy.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import hudson.plugins.octopusdeploy.Log;
import hudson.plugins.octopusdeploy.OctopusPackageMetadata;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class OctopusMetadataWriter {

    private final Boolean verboseLogging;
    private final Log log;

    public OctopusMetadataWriter(Log log, Boolean verboseLogging) {
        this.log = log;
        this.verboseLogging = verboseLogging;
    }

    public void writeToFile(final OctopusPackageMetadata octopusPackageMetadata, final String metaFile) throws IOException {
        try {
            final Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create();
            if (verboseLogging) {
                log.info("Serializing Octopus metadata");
            }

            final String jsonData = gson.toJson(octopusPackageMetadata);
            if (verboseLogging) {
                log.info("Serialized Octopus metadata - " + jsonData);
            }
            OutputStreamWriter bw = new OutputStreamWriter(new FileOutputStream(metaFile), StandardCharsets.UTF_16);
            bw.write(jsonData);
            bw.close();

            if (verboseLogging) {
                log.info("Wrote " + metaFile);
            }

        } catch (IOException e) {
            e.printStackTrace();
            log.error("Error writing octopus.metadata file");
            throw e;
        }
    }
}