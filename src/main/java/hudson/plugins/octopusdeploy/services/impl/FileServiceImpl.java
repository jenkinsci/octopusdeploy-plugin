package hudson.plugins.octopusdeploy.services.impl;


import hudson.FilePath;
import hudson.model.Computer;
import hudson.plugins.octopusdeploy.exception.ResourceException;
import hudson.plugins.octopusdeploy.services.FileService;
import hudson.slaves.WorkspaceList;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Paths;
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
    public List<FilePath> getMatchingFile(@NotNull final FilePath workingDir, @NotNull final String pattern) {
        checkNotNull(workingDir);
        checkArgument(StringUtils.isNotBlank(pattern));
        String p = pattern;
        if (pattern.startsWith("/") || (pattern.startsWith("/") && !pattern.startsWith("//"))) {
            // leading slashes are not valid glob patterns, remove them
            p = pattern.replaceAll("^/+", "").replaceAll("^\\+", "");
        }

        final File absoluteFile = new File(pattern);
        if (absoluteFile.exists()) {
            return new ArrayList<FilePath>() {{
                add(new FilePath(absoluteFile));
            }};
        }

        List<FilePath> list;
        try {
            list = Arrays.asList(workingDir.list(p));
        } catch (final Exception ex) {
            throw new ResourceException(ex);
        }

        return list;
    }
}
