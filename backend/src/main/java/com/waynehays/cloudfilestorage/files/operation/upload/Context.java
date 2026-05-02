package com.waynehays.cloudfilestorage.files.operation.upload;

import com.waynehays.cloudfilestorage.files.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.files.dto.response.ResourceDto;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class Context {
    private final List<UploadObjectDto> objects;

    @Getter
    private final Long userId;

    @Getter
    private final long totalSize;

    private final List<ResourceDto> result = Collections.synchronizedList(new ArrayList<>());
    private final List<String> uploadedStorageKeys = Collections.synchronizedList(new ArrayList<>());
    private final List<String> savedToDbPaths = Collections.synchronizedList(new ArrayList<>());

    private volatile boolean quotaReserved;

    Context(Long userId, List<UploadObjectDto> objects) {
        this.userId = userId;
        this.objects = List.copyOf(objects);
        this.totalSize = objects.stream().mapToLong(UploadObjectDto::size).sum();
    }

    void addToResult(List<ResourceDto> items) {
        result.addAll(items);
    }

    void addToResult(ResourceDto item) {
        result.add(item);
    }

    List<ResourceDto> getResult() {
        return List.copyOf(result);
    }

    List<UploadObjectDto> getObjects() {
        return List.copyOf(objects);
    }

    List<String> getAllPaths() {
        return objects.stream()
                .map(UploadObjectDto::fullPath)
                .toList();
    }

    void addSavedToDbPath(String path) {
        savedToDbPaths.add(path);
    }

    void addUploadedStorageKey(String key) {
        uploadedStorageKeys.add(key);
    }

    void markQuotaReserved() {
        quotaReserved = true;
    }

    void markAllPathsSavedToDb() {
        objects.forEach(o -> savedToDbPaths.add(o.fullPath()));
    }

    RollbackDto rollbackDto() {
        return new RollbackDto(
                userId,
                totalSize,
                quotaReserved,
                List.copyOf(uploadedStorageKeys),
                List.copyOf(savedToDbPaths)
        );
    }
}
