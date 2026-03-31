package com.waynehays.cloudfilestorage.dto.request.directory;

import com.waynehays.cloudfilestorage.annotation.ValidPath;

public record GetDirectoryContentRequest(

        @ValidPath(mustBeDirectory = true, message = "Path must end with /")
        String path
) {
}
