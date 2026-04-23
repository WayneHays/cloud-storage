package com.waynehays.cloudfilestorage.resource.service.uploading;

import com.waynehays.cloudfilestorage.resource.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.resource.dto.response.ResourceDto;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class UploadContext {
    private final List<UploadObjectDto> objects;

    @Getter
    private final Long userId;

    @Getter
    private final long totalSize;

    private final List<ResourceDto> result = Collections.synchronizedList(new ArrayList<>());
    private final List<String> uploadedToStoragePaths = Collections.synchronizedList(new ArrayList<>());
    private final List<String> savedToDbPaths = Collections.synchronizedList(new ArrayList<>());

    private volatile boolean quotaReserved;

    UploadContext(Long userId, List<UploadObjectDto> objects) {
        this.userId = userId;
        this.objects = List.copyOf(objects);
        this.totalSize = objects.stream().mapToLong(UploadObjectDto::size).sum();
    }

    void addResult(List<ResourceDto> items) {
        result.addAll(items);
    }

    void addResult(ResourceDto item) {
        result.add(item);
    }

    List<ResourceDto> getResult() {
        return List.copyOf(result);
    }

    List<UploadObjectDto> getObjects() {
        return List.copyOf(objects);
    }

    void addSavedToDbPath(String path) {
        savedToDbPaths.add(path);
    }

    void addUploadedToStoragePath(String path) {
        uploadedToStoragePaths.add(path);
    }

    void markQuotaReserved() {
        quotaReserved = true;
    }

    UploadRollbackSnapshot rollbackSnapshot() {
        return new UploadRollbackSnapshot(
                userId,
                totalSize,
                quotaReserved,
                List.copyOf(uploadedToStoragePaths),
                List.copyOf(savedToDbPaths)
        );
    }
}
