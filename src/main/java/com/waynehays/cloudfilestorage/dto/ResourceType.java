package com.waynehays.cloudfilestorage.dto;

public enum ResourceType {
    DIRECTORY,
    FILE;

    public boolean isDirectory() {
        return this == DIRECTORY;
    }

    public boolean isFile() {
        return this == FILE;
    }
}
