package com.waynehays.cloudfilestorage.parser.querypathparser;

import com.waynehays.cloudfilestorage.dto.files.ParsedPath;

public interface QueryPathParser {
    ParsedPath parse(String queryPath);
}
