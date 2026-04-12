package com.waynehays.cloudfilestorage.service.resource.move;

import com.waynehays.cloudfilestorage.dto.internal.metadata.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.exception.ApplicationException;
import com.waynehays.cloudfilestorage.exception.InvalidMoveException;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.ResourceStorageOperationException;
import com.waynehays.cloudfilestorage.mapper.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.storage.ResourceStorageService;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceMoveService implements ResourceMoveServiceApi {
    private final ResourceStorageService storageService;
    private final ExecutorService moveExecutor;
    private final ResourceDtoMapper mapper;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public ResourceDto move(Long userId, String pathFrom, String pathTo) {
        validateMove(userId, pathFrom, pathTo);
        ResourceMetadataDto dto = metadataService.findOrThrow(userId, pathFrom);

        if (dto.isFile()) {
            moveFile(userId, pathFrom, pathTo);
            return mapper.fileFromPath(pathTo, dto.size());
        }

        moveDirectory(userId, pathFrom, pathTo);
        return mapper.directoryFromPath(pathTo);
    }

    private void validateMove(Long userId, String pathFrom, String pathTo) {
        if (PathUtils.isDirectory(pathFrom)) {
            if (PathUtils.isFile(pathTo)) {
                throw new InvalidMoveException("Cannot move directory to file", pathFrom, pathTo);
            }
            if (pathTo.startsWith(pathFrom)) {
                throw new InvalidMoveException("Cannot move directory into itself", pathFrom, pathTo);
            }
        }

        Set<String> existing = metadataService.findExistingPaths(userId, Set.of(pathTo));

        if (!existing.isEmpty()) {
            throw new ResourceAlreadyExistsException("Resource already exists at target path", pathTo);
        }
    }

    private void moveFile(Long userId, String pathFrom, String pathTo) {
        log.info("Start move file: userId={}, from={}, to={}", userId, pathFrom, pathTo);
        MoveContext context = new MoveContext();

        try {
            storageService.moveObject(userId, pathFrom, pathTo);
            context.addMovedObject(pathFrom, pathTo);
            metadataService.updatePathsByPrefix(userId, pathFrom, pathTo);
        } catch (Exception e) {
            rollback(userId, context);
            throw e;
        }

        log.info("Successfully moved file: userId={}, from={}, to={}", userId, pathFrom, pathTo);
    }

    private void moveDirectory(Long userId, String pathFrom, String pathTo) {
        log.info("Start move directory: userId={}, from={}, to={}", userId, pathFrom, pathTo);

        List<ResourceMetadataDto> files = metadataService.findFilesByPathPrefix(userId, pathFrom);
        MoveContext context = new MoveContext();

        try {
            moveContent(userId, files, pathFrom, pathTo, context);
            metadataService.updatePathsByPrefix(userId, pathFrom, pathTo);
        } catch (Exception e) {
            log.warn("Move failed for userId={}, initiating rollback", userId);
            rollback(userId, context);
            throw e;
        }

        log.info("Successfully moved directory: userId={}, from={}, to={}", userId, pathFrom, pathTo);
    }

    private void moveContent(Long userId, List<ResourceMetadataDto> files,
                             String pathFrom, String pathTo, MoveContext context) {
        List<CompletableFuture<Void>> futures = files.stream()
                .map(f -> CompletableFuture.runAsync(() -> {
                    String newPath = calculateNewPath(pathFrom, pathTo, f.path());
                    storageService.moveObject(userId, f.path(), newPath);
                    context.addMovedObject(f.path(), newPath);
                }, moveExecutor))
                .toList();
        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ApplicationException ae) {
                throw ae;
            }
            throw new ResourceStorageOperationException("Failed to move some files in storage", cause);
        }
    }

    private String calculateNewPath(String pathFrom, String pathTo, String filePath) {
        return pathTo + filePath.substring(pathFrom.length());
    }

    private void rollback(Long userId, MoveContext context) {
        context.getMovedObjects().forEach(m -> {
            try {
                storageService.moveObject(userId, m.pathTo(), m.pathFrom());
            } catch (Exception e) {
                log.error("Failed to rollback move: {} -> {}", m.pathTo(), m.pathFrom(), e);
            }
        });
    }
}
