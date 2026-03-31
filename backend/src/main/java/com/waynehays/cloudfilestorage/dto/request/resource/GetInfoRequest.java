package com.waynehays.cloudfilestorage.dto.request.resource;

import com.waynehays.cloudfilestorage.annotation.ValidPath;
import jakarta.validation.constraints.NotBlank;

public record GetInfoRequest(

        @NotBlank(message = "Path cannot be blank")
        @ValidPath
        String path
) {
}
