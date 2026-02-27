package com.waynehays.cloudfilestorage.dto.file;

import com.waynehays.cloudfilestorage.dto.file.response.ResourceType;

public record ResourcePath(
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
