package com.waynehays.cloudfilestorage.core.metadata;

public enum ResourceType {
    DIRECTORY,
    FILE;

    public boolean isFile() {
        return this == FILE;
    }
}
