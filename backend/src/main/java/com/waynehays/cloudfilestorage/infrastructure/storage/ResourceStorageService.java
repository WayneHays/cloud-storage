package com.waynehays.cloudfilestorage.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
class ResourceStorageService implements ResourceStorageServiceApi {
    private static final String KEY_TEMPLATE = "user-%d-files/";

    private final ResourceStorageApi storage;

    @Override
    public void putObject(Long userId, String path, long size, String contentType, InputStreamSupplier inputStreamSupplier) {
        log.debug("Start upload object to storage: path={}", path);

        String key = resolveKey(userId, path);

        try (InputStream inputStream = inputStreamSupplier.get()) {
            storage.putObject(inputStream, key, size, contentType);
        } catch (IOException e) {
            throw new ResourceStorageException("Failed to put object to storage", e);
        }

        log.debug("Finished upload object to storage: path={}", path);
    }

    @Override
    public Optional<StorageItem> getObject(Long userId, String path) {
        String key = resolveKey(userId, path);
        return storage.getObject(key);
    }

    @Override
    public void deleteObject(Long userId, String path) {
        log.debug("Start delete object from storage: path={}", path);

        String key = resolveKey(userId, path);
        storage.deleteObject(key);

        log.debug("Finished delete object from storage: path={}", path);
    }

    @Override
    public void deleteObjects(Map<Long, List<String>> pathsByUserId) {
        log.debug("Start batch delete objects from storage");

        List<String> keys = pathsByUserId.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(path -> resolveKey(entry.getKey(), path)))
                .toList();
        storage.deleteList(keys);

        log.debug("Finished batch delete objects from storage: {} objects removed", keys.size());
    }

    @Override
    public void deleteByPrefix(Long userId, String path) {
        log.debug("Start delete objects by key prefix from storage");

        String key = resolveKey(userId, path);
        storage.deleteByPrefix(key);

        log.debug("Finished delete objects by key prefix from storage");
    }

    @Override
    public void moveObject(Long userId, String pathFrom, String pathTo) {
        log.debug("Start move object in storage: from={}, to={}", pathFrom, pathTo);

        String keyFrom = resolveKey(userId, pathFrom);
        String keyTo = resolveKey(userId, pathTo);
        storage.moveObject(keyFrom, keyTo);

        log.debug("Finished move object in storage: from={}, to={}", pathFrom, pathTo);
    }

    private String resolveKey(Long userId, String path) {
        return KEY_TEMPLATE.formatted(userId) + path;
    }
}
