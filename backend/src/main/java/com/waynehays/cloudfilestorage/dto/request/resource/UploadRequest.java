package com.waynehays.cloudfilestorage.dto.request.resource;

import com.waynehays.cloudfilestorage.annotation.ValidPath;

public record UploadRequest(

        @ValidPath(mustBeDirectory = true, message = "Path must end with /")
        String path
) {
}
