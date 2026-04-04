package com.waynehays.cloudfilestorage.service.resource.uploader;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
class UploadContext {
    private final List<String> storagePaths = Collections.synchronizedList(new ArrayList<>());
    private final List<String> metadataPaths = Collections.synchronizedList(new ArrayList<>());

    void addStoragePath(String path) {
        storagePaths.add(path);
    }

    void addMetadataPath(String path) {
        metadataPaths.add(path);
    }

    boolean containsStoragePaths() {
        return !storagePaths.isEmpty();
    }

    boolean containsMetadataPaths() {
        return !metadataPaths.isEmpty();
    }
}
