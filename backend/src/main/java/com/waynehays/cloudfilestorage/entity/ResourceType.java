package com.waynehays.cloudfilestorage.entity;

public enum ResourceType {
    DIRECTORY,
    FILE;

    public boolean isFile() {
        return this == FILE;
    }
}
