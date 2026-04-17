package com.waynehays.cloudfilestorage.service.storage;

import com.waynehays.cloudfilestorage.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.dto.internal.storage.StorageItem;
import com.waynehays.cloudfilestorage.dto.internal.storage.UserPath;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.exception.ResourceStorageOperationException;
import com.waynehays.cloudfilestorage.infrastructure.storage.KeyResolverApi;
import com.waynehays.cloudfilestorage.infrastructure.storage.ResourceStorageApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceStorageService implements ResourceStorageServiceApi {
    private final ResourceStorageApi storage;
    private final KeyResolverApi keyResolver;

    @Override
    public void putObject(Long userId, UploadObjectDto uploadObject) {
        String path = uploadObject.fullPath();
        log.debug("Start upload object to storage: userId={}, path={}", userId, path);

        String key = keyResolver.resolveKey(userId, path);
        try (InputStream inputStream = uploadObject.inputStreamSupplier().get()) {
            storage.putObject(inputStream, key, uploadObject.size(), uploadObject.contentType());
        } catch (IOException e) {
            throw new ResourceStorageOperationException("Failed to put object to storage", e);
        }

        log.debug("Finished upload object to storage: userId={}, path={}", userId, path);
    }

    @Override
    public StorageItem getObject(Long userId, String path) {
        String key = keyResolver.resolveKey(userId, path);
        return storage.getObject(key)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found in storage", path));
    }

    @Override
    public void deleteObject(Long userId, String path) {
        log.debug("Start delete object from storage: userId={}, path={}", userId, path);

        String key = keyResolver.resolveKey(userId, path);
        storage.deleteObject(key);

        log.debug("Finished delete object from storage: userId={}, path={}", userId, path);
    }

    @Override
    public void deleteObjects(List<UserPath> userPaths) {
        log.debug("Start delete list objects from storage");

        List<String> keys = userPaths.stream()
                .map(up -> keyResolver.resolveKey(up.userId(), up.path()))
                .toList();
        storage.deleteList(keys);

        log.debug("Finished delete list objects from storage");
    }

    @Override
    public void deleteDirectory(Long userId, String path) {
        log.debug("Start delete directory content from storage: userId={}, path to directory={}", userId, path);

        String key = keyResolver.resolveKey(userId, path);
        storage.deleteByPrefix(key);

        log.debug("Finished delete directory content from storage: userId={}, path to directory={}", userId, path);
    }

    @Override
    public void moveObject(Long userId, String pathFrom, String pathTo) {
        log.debug("Start move object in storage: userId={}, from={}, to={}", userId, pathFrom, pathTo);

        String keyFrom = keyResolver.resolveKey(userId, pathFrom);
        String keyTo = keyResolver.resolveKey(userId, pathTo);
        storage.moveObject(keyFrom, keyTo);

        log.debug("Finished move object in storage: userId={}, from={}, to={}", userId, pathFrom, pathTo);
    }
}
