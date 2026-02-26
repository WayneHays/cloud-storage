package com.waynehays.cloudfilestorage.parser.resourcepathparser;

import com.waynehays.cloudfilestorage.constant.Constants;
import com.waynehays.cloudfilestorage.dto.files.ResourcePath;
import com.waynehays.cloudfilestorage.dto.files.response.ResourceType;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import com.waynehays.cloudfilestorage.validator.PathValidator;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResourcePathParserImpl implements ResourcePathParser {

    private final PathValidator pathValidator;

    @Override
    public ResourcePath parse(String queryPath) {
        pathValidator.validateQueryPath(queryPath);

        if (StringUtils.isBlank(queryPath)) {
            return pathToDirectory(Constants.ROOT_DIRECTORY);
        }
        String normalizedPath = PathUtils.normalizeSeparators(queryPath.trim());

        if (PathUtils.endsWithSeparator(normalizedPath)) {
            return pathToDirectory(PathUtils.removeTrailingSeparator(normalizedPath));
        }

        String directory = PathUtils.removeTrailingSeparator(PathUtils.extractParentPath(normalizedPath));
        String fileName = PathUtils.extractFilename(normalizedPath);

        return new ResourcePath(directory, fileName, ResourceType.FILE);
    }

    private ResourcePath pathToDirectory(String path) {
        return new ResourcePath(path, null, ResourceType.DIRECTORY);
    }
}
