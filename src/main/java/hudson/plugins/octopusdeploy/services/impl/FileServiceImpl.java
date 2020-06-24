package hudson.plugins.octopusdeploy.services.impl;

import hudson.FilePath;
import hudson.plugins.octopusdeploy.Log;
import hudson.plugins.octopusdeploy.exception.ResourceException;
import hudson.plugins.octopusdeploy.services.FileService;
import hudson.remoting.VirtualChannel;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.remoting.RoleChecker;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An implementation of the file service using Spring utils
 */
@Component
public class FileServiceImpl implements FileService {
    @NotNull
    @Override
    public List<FilePath> getMatchingFile(@NotNull final FilePath workingDir, @NotNull final String pattern, @NotNull Log log) {
        checkNotNull(workingDir);
        checkArgument(StringUtils.isNotBlank(pattern));

        try
        {
            File absoluteFile = workingDir.act(new CheckFileExistsOnNode(pattern));
            if(absoluteFile != null)
            {
                return new ArrayList<FilePath>() {{
                    add(new FilePath(workingDir.getChannel(), absoluteFile.getPath()));
                }};
            }
        } catch (Exception e) { /* don't need to worry if it fails, just fall back to using an ant glob */ }

        String p = pattern;
        if (pattern.startsWith("/") || (pattern.startsWith("/") && !pattern.startsWith("//"))) {
            // leading slashes are not valid glob patterns, remove them
            p = pattern.replaceAll("^/+", "").replaceAll("^\\+", "");
        }

        List<FilePath> list;
        try {
            list = Arrays.asList(workingDir.list(p));
        } catch (final Exception ex) {
            log.info("If supplying an absolute path to a file, it's likely your file doesn't exist.");
            throw new ResourceException(ex);
        }

        return list;
    }

    private static final class CheckFileExistsOnNode implements FilePath.FileCallable<File> {
        private final String pattern;

        public CheckFileExistsOnNode(String pattern)
        {
            this.pattern = pattern;
        }

        @Override public File invoke(File f, VirtualChannel channel) {
            // f and file represent the same thing
            final File absoluteFile = new File(pattern);
            if(absoluteFile.exists())
            {
                return absoluteFile;
            }

            return null;
        }

        @Override
        public void checkRoles(RoleChecker roleChecker) throws SecurityException {
        }
    }
}
