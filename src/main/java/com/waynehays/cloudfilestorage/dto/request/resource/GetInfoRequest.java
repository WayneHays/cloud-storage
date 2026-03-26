package com.waynehays.cloudfilestorage.dto.request.resource;

import com.waynehays.cloudfilestorage.annotation.ValidPath;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GetInfoRequest(

        @NotBlank(message = "Path cannot be blank")
        @Size(max = 200, message = "Path cannot be more than 200 symbols")
        @ValidPath
        String path
) {
}
