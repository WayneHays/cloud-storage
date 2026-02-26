package com.waynehays.cloudfilestorage.parser.querypathparser;

import com.waynehays.cloudfilestorage.constant.Constants;
import com.waynehays.cloudfilestorage.dto.files.ParsedPath;
import com.waynehays.cloudfilestorage.dto.files.response.ResourceType;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import com.waynehays.cloudfilestorage.validator.PathValidator;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QueryPathParserImpl implements QueryPathParser {

    private final PathValidator pathValidator;

    @Override
    public ParsedPath parse(String queryPath) {
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

        return new ParsedPath(directory, fileName, ResourceType.FILE);
    }

    private ParsedPath pathToDirectory(String path) {
        return new ParsedPath(path, null, ResourceType.DIRECTORY);
    }
}
