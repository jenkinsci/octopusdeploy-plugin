package hudson.plugins.octopusdeploy.services;


import hudson.FilePath;
import hudson.plugins.octopusdeploy.Log;
import org.jetbrains.annotations.NotNull;
import java.util.List;

/**
 * Defines a service that is used to work with files and dirs
 */
public interface FileService {
    /**
     * @param workingDir The directory from which to start matching files
     * @param pattern    The ant pattern used to match files
     * @return A list of matching files
     */
    @NotNull
    List<FilePath> getMatchingFile(@NotNull FilePath workingDir, @NotNull String pattern, Log log);
}
