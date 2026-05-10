package com.waynehays.cloudfilestorage.files.operation.upload.dto;

import com.waynehays.cloudfilestorage.files.api.dto.response.ResourceResponse;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Context {
    private final List<UploadObjectDto> objects;

    @Getter
    private final Long userId;

    @Getter
    private final long totalSize;

    private final List<ResourceResponse> result = Collections.synchronizedList(new ArrayList<>());
    private final List<String> uploadedStorageKeys = Collections.synchronizedList(new ArrayList<>());
    private final List<String> savedToDbPaths = Collections.synchronizedList(new ArrayList<>());

    private volatile boolean quotaReserved;

    public Context(Long userId, List<UploadObjectDto> objects) {
        this.userId = userId;
        this.objects = List.copyOf(objects);
        this.totalSize = objects.stream().mapToLong(UploadObjectDto::size).sum();
    }

    public void addToResult(List<ResourceResponse> items) {
        result.addAll(items);
    }

    public void addToResult(ResourceResponse item) {
        result.add(item);
    }

    public List<ResourceResponse> getResult() {
        return List.copyOf(result);
    }

    public List<UploadObjectDto> getObjects() {
        return List.copyOf(objects);
    }

    public List<String> getAllPaths() {
        return objects.stream()
                .map(UploadObjectDto::fullPath)
                .toList();
    }

    public void addSavedToDbPath(String path) {
        savedToDbPaths.add(path);
    }

    public void addUploadedStorageKey(String key) {
        uploadedStorageKeys.add(key);
    }

    public void markQuotaReserved() {
        quotaReserved = true;
    }

    public void markAllPathsSavedToDb() {
        objects.forEach(o -> savedToDbPaths.add(o.fullPath()));
    }

    public RollbackDto rollbackDto() {
        return new RollbackDto(
                userId,
                totalSize,
                quotaReserved,
                List.copyOf(uploadedStorageKeys),
                List.copyOf(savedToDbPaths)
        );
    }
}
