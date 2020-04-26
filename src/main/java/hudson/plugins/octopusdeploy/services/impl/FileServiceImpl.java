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

        List<FilePath> list = new ArrayList<FilePath>();
        try {
            list = Arrays.asList(workingDir.list(pattern));
        } catch (final Exception ex) {
            throw new ResourceException(ex);
        }

        return list;
    }
}
