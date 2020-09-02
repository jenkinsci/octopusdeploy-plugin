package hudson.plugins.octopusdeploy.exception;

public class ServerConfigurationNotFoundException extends Exception {
    public ServerConfigurationNotFoundException(String serverId) {
        super(
                serverId == null || serverId.isEmpty()
                        ? "Server Id not supplied and no default server could be found"
                        : "Server configuration, '" + serverId + "', not found. The serverId parameter needs to match the Server ID of a configured OctopusDeploy server at Manage Jenkins -> Configure System -> OctopusDeploy Plugin");
    }
}

