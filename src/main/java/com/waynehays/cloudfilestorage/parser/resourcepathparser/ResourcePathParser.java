package com.waynehays.cloudfilestorage.parser.resourcepathparser;

import com.waynehays.cloudfilestorage.dto.file.ResourcePath;

public interface ResourcePathParser {
    ResourcePath parse(String queryPath);
}
