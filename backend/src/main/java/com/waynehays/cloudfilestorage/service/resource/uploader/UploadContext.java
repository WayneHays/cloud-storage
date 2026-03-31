package com.waynehays.cloudfilestorage.service.resource.uploader;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
class UploadContext {
    private final List<String> storageKeys = new ArrayList<>();
    private final List<String> metadataPaths = new ArrayList<>();

    void addStorageKey(String key) {
        storageKeys.add(key);
    }

    void addMetadataPath(String path) {
        metadataPaths.add(path);
    }
}
