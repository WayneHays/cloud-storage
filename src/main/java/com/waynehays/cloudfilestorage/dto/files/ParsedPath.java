package com.waynehays.cloudfilestorage.dto.files;

import com.waynehays.cloudfilestorage.dto.files.response.ResourceType;

public record ParsedPath(
        String directory,
        String filename,
        ResourceType type
) {
    public boolean isFile() {
        return type == ResourceType.FILE;
    }

    public boolean isDirectory() {
        return type == ResourceType.DIRECTORY;
    }
}
