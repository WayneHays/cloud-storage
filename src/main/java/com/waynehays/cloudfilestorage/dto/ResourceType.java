package com.waynehays.cloudfilestorage.dto;

public enum ResourceType {
    DIRECTORY,
    FILE;

    public boolean isFile() {
        return this == FILE;
    }
}
