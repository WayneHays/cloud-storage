package com.waynehays.cloudfilestorage.resource.entity;

public enum ResourceType {
    DIRECTORY,
    FILE;

    public boolean isFile() {
        return this == FILE;
    }
}
