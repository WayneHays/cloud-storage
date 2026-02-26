package com.waynehays.cloudfilestorage.parser.resourcepathparser;

import com.waynehays.cloudfilestorage.dto.files.ResourcePath;

public interface ResourcePathParser {
    ResourcePath parse(String queryPath);
}
