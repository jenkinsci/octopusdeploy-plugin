package com.octopusdeploy.api.data;

/**
 * Represents a SelectedPackage, part of a Release.
 */
public class SelectedPackage {

    private String stepName;
    public String getStepName() {
        return stepName;
    }
    public void setStepName(String stepName) { this.stepName = stepName; }

    private final String packageId;
    public String getPackageId() { return packageId; }

    private final String packageReferenceName;
    public String getPackageReferenceName() { return packageReferenceName; }

    private final String version;
    public String getVersion() { return version; }

    public SelectedPackage(String stepName, String packageId, String packageReferenceName, String version) {
        this.stepName = stepName;
        this.packageId = packageId;
        this.packageReferenceName = packageReferenceName;
        this.version = version;
    }

    @Override
    public String toString() {
        return "SelectedPackage [stepName=" + stepName + ", packageId=" + packageId + ", packageReferenceName=" + packageReferenceName + ", version=" + version + "]";
    }

}
