package com.octopusdeploy.api;

/**
 * Represents a SelectedPackage, part of a Release.
 */
public class SelectedPackage {
    private final String stepName;
    public String getStepName() {
        return stepName;
    }
    
    private final String version;
    public String getVersion() {
        return version;
    }
    
    public SelectedPackage(String stepName, String version) {
        this.stepName = stepName;
        this.version = version;
    }
}
