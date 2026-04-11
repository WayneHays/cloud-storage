package com.waynehays.cloudfilestorage.dto.response;

import com.waynehays.cloudfilestorage.entity.ResourceType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resource information")
public record ResourceDto(

        @Schema(description = "Path to parent directory containing this resource",
                example = "docs/")
        String path,

        @Schema(description = "Resource name. Directories end with '/'",
                example = "file.txt")
        String name,

        @Schema(description = "File size in bytes. Null for directories",
                example = "2048")
        Long size,

        @Schema(description = "Resource type",
                example = "FILE")
        ResourceType type
) {
}
