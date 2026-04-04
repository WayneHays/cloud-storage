package com.waynehays.cloudfilestorage.storage.provider;

import com.waynehays.cloudfilestorage.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.exception.ResourceStorageOperationException;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageKeyResolverApi;
import com.waynehays.cloudfilestorage.storage.dto.StorageItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ResourceStorageService {
    private final ResourceStorageApi storage;
    private final ResourceStorageKeyResolverApi keyResolver;

    public void putObject(Long userId, UploadObjectDto uploadObject) {
        String key = keyResolver.resolveKey(userId, uploadObject.fullPath());
        try (InputStream inputStream = uploadObject.inputStreamSupplier().get()) {
            storage.putObject(inputStream, key, uploadObject.size(), uploadObject.contentType());
        } catch (IOException e) {
            throw new ResourceStorageOperationException("Failed to put object to storage", e);
        }
    }

    public Optional<StorageItem> getObject(Long userId, String path) {
        String key = keyResolver.resolveKey(userId, path);
        return storage.getObject(key);
    }

    public InputStream getInputStream(Long userId, String path) {
        String key = keyResolver.resolveKey(userId, path);
        return storage.getObject(key)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found in storage", path))
                .inputStream();
    }

    public void deleteObject(Long userId, String path) {
        String key = keyResolver.resolveKey(userId, path);
        storage.deleteObject(key);
    }

    public void deleteObjects(Long userId, List<String> paths) {
        List<String> keys = paths.stream()
                .map(path -> keyResolver.resolveKey(userId, path))
                .toList();
        storage.deleteList(keys);
    }

    public void deleteDirectory(Long userId, String path) {
        String key = keyResolver.resolveKey(userId, path);
        storage.deleteByPrefix(key);
    }

    public void moveObject(Long userId, String pathFrom, String pathTo) {
        String keyFrom = keyResolver.resolveKey(userId, pathFrom);
        String keyTo = keyResolver.resolveKey(userId, pathTo);
        storage.moveObject(keyFrom, keyTo);
    }
}
