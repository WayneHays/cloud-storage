package com.waynehays.cloudfilestorage.service.resource.mover;

import com.waynehays.cloudfilestorage.dto.internal.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.exception.InvalidMoveException;
import com.waynehays.cloudfilestorage.exception.ResourceStorageOperationException;
import com.waynehays.cloudfilestorage.mapper.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.storage.ResourceStorageService;
import com.waynehays.cloudfilestorage.utils.PathUtils;
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
    private final ResourceStorageService storageService;
    private final ExecutorService moveExecutor;
    private final ResourceDtoMapper mapper;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public ResourceDto move(Long userId, String pathFrom, String pathTo) {
        validateMove(pathFrom, pathTo);
        ResourceMetadataDto dto = metadataService.findOrThrow(userId, pathFrom);

        if (dto.isFile()) {
            moveFile(userId, pathFrom, pathTo);
            return mapper.fileFromPath(pathTo, dto.size());
        }

        moveDirectory(userId, pathFrom, pathTo);
        return mapper.directoryFromPath(pathTo);
    }

    private void moveFile(Long userId, String pathFrom, String pathTo) {
        log.info("Start move file: userId={}, from={}, to={}", userId, pathFrom, pathTo);

        storageService.moveObject(userId, pathFrom, pathTo);
        metadataService.updatePathsByPrefix(userId, pathFrom, pathTo);

        log.info("Successfully moved file: userId={}, from={}, to={}", userId, pathFrom, pathTo);
    }

    private void moveDirectory(Long userId, String pathFrom, String pathTo) {
        log.info("Start move directory: userId={}, from={}, to={}", userId, pathFrom, pathTo);

        List<ResourceMetadataDto> files = metadataService.findFilesByPathPrefix(userId, pathFrom);
        moveContent(userId, files, pathFrom, pathTo);
        metadataService.updatePathsByPrefix(userId, pathFrom, pathTo);

        log.info("Successfully moved directory: userId={}, from={}, to={}", userId, pathFrom, pathTo);
    }

    private void moveContent(Long userId, List<ResourceMetadataDto> files, String pathFrom, String pathTo) {
        List<CompletableFuture<Void>> futures = files.stream()
                        .map(f -> CompletableFuture.runAsync(() -> {
                            String newPath = calculateNewPath(pathFrom, pathTo, f.path());
                            storageService.moveObject(userId, f.path(), newPath);
                        }, moveExecutor))
                                .toList();
        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        } catch (CompletionException e) {
            throw new ResourceStorageOperationException("Failed to move some files in storage", e);
        }
    }

    private String calculateNewPath(String pathFrom, String pathTo, String filePath) {
        return pathTo + filePath.substring(pathFrom.length());
    }

    private void validateMove(String pathFrom, String pathTo) {
        if (PathUtils.isDirectory(pathFrom) && PathUtils.isFile(pathTo)) {
            throw new InvalidMoveException("Cannot move directory to file", pathFrom, pathTo);
        }

        if (PathUtils.isDirectory(pathFrom) && pathTo.startsWith(pathFrom)) {
            throw new InvalidMoveException("Cannot move directory into itself", pathFrom, pathTo);
        }
    }
}
