package com.waynehays.cloudfilestorage.service.resource.upload;

import com.waynehays.cloudfilestorage.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
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

    private final List<ResourceDto> result = new ArrayList<>();
    private final List<String> uploadedToStoragePaths = Collections.synchronizedList(new ArrayList<>());
    private final List<String> savedToDbPaths = Collections.synchronizedList(new ArrayList<>());

    private volatile boolean quotaReserved;

    UploadContext(Long userId, List<UploadObjectDto> objects) {
        this.userId = userId;
        this.objects = objects;
        this.totalSize = objects.stream().mapToLong(UploadObjectDto::size).sum();
    }

    void addResult(List<ResourceDto> items) {
        result.addAll(items);
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

    RollbackSnapshot rollbackSnapshot() {
        return new RollbackSnapshot(
                userId,
                totalSize,
                quotaReserved,
                List.copyOf(uploadedToStoragePaths),
                List.copyOf(savedToDbPaths)
        );
    }
}
