package com.waynehays.cloudfilestorage.service.resource.mover;

import com.waynehays.cloudfilestorage.component.ResourceDtoConverter;
import com.waynehays.cloudfilestorage.component.validator.MoveValidator;
import com.waynehays.cloudfilestorage.dto.internal.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.exception.ResourceStorageOperationException;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageKeyResolverApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceMover implements ResourceMoverApi {
    private final MoveValidator validator;
    private final ExecutorService moveExecutor;
    private final ResourceDtoConverter dtoConverter;
    private final ResourceStorageApi resourceStorage;
    private final ResourceStorageKeyResolverApi keyResolver;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public ResourceDto move(Long userId, String pathFrom, String pathTo) {
        validator.validate(userId, pathFrom, pathTo);
        ResourceMetadataDto dto = metadataService.findOrThrow(userId, pathFrom);

        if (dto.isFile()) {
            moveFile(userId, pathFrom, pathTo);
            return dtoConverter.fileFromPath(pathTo, dto.size());
        }

        moveDirectory(userId, pathFrom, pathTo);
        return dtoConverter.directoryFromPath(pathTo);
    }

    private void moveFile(Long userId, String pathFrom, String pathTo) {
        log.info("Start move file: userId={}, from={}, to={}", userId, pathFrom, pathTo);

        String keyFrom = keyResolver.resolveKey(userId, pathFrom);
        String keyTo = keyResolver.resolveKey(userId, pathTo);
        resourceStorage.moveObject(keyFrom, keyTo);
        metadataService.updatePathsByPrefix(userId, pathFrom, pathTo);

        log.info("Successfully moved file: userId={}, from={}, to={}", userId, pathFrom, pathTo);
    }

    private void moveDirectory(Long userId, String pathFrom, String pathTo) {
        log.info("Start move directory: userId={}, from={}, to={}", userId, pathFrom, pathTo);

        List<ResourceMetadataDto> files = metadataService.findFilesByPrefix(userId, pathFrom);
        moveContent(userId, files, pathFrom, pathTo);
        metadataService.updatePathsByPrefix(userId, pathFrom, pathTo);

        log.info("Successfully moved directory: userId={}, from={}, to={}", userId, pathFrom, pathTo);
    }

    private void moveContent(Long userId, List<ResourceMetadataDto> files, String pathFrom, String pathTo) {
        List<CompletableFuture<Void>> futures = files.stream()
                        .map(f -> CompletableFuture.runAsync(() -> {
                            String oldKey = keyResolver.resolveKey(userId, f.path());
                            String newPath = f.path().replaceFirst(pathFrom, pathTo);
                            String newKey = keyResolver.resolveKey(userId, newPath);
                            resourceStorage.moveObject(oldKey, newKey);
                        }, moveExecutor))
                                .toList();
        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        } catch (CompletionException e) {
            throw new ResourceStorageOperationException("Failed to move some files in storage", e);
        }
    }
}
