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
    public void putObject(Long userId, String storageKey, long size, String contentType, InputStreamSupplier inputStreamSupplier) {
        log.debug("Start upload object to storage: path={}", storageKey);

        String key = resolveKey(userId, storageKey);

        try (InputStream inputStream = inputStreamSupplier.get()) {
            storage.putObject(inputStream, key, size, contentType);
        } catch (IOException e) {
            throw new ResourceStorageException("Failed to put object to storage", e);
        }

        log.debug("Finished upload object to storage: path={}", storageKey);
    }

    @Override
    public Optional<StorageItem> getObject(Long userId, String storageKey) {
        String key = resolveKey(userId, storageKey);
        return storage.getObject(key);
    }

    @Override
    public void deleteObject(Long userId, String storageKey) {
        log.debug("Start delete object from storage: path={}", storageKey);

        String key = resolveKey(userId, storageKey);
        storage.deleteObject(key);

        log.debug("Finished delete object from storage: path={}", storageKey);
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

    private String resolveKey(Long userId, String path) {
        return KEY_TEMPLATE.formatted(userId) + path;
    }
}
