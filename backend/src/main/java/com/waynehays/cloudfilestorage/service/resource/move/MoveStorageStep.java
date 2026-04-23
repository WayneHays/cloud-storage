package com.waynehays.cloudfilestorage.service.resource.move;

import com.waynehays.cloudfilestorage.dto.internal.metadata.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.exception.ApplicationException;
import com.waynehays.cloudfilestorage.exception.ResourceStorageOperationException;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.storage.ResourceStorageServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

@Slf4j
@Component
@RequiredArgsConstructor
class MoveStorageStep implements MoveStep {
    private final ResourceStorageServiceApi storageService;
    private final ResourceMetadataServiceApi metadataService;
    private final ExecutorService moveExecutor;

    @Override
    public void execute(MoveContext context) {
        if (context.isFile()) {
            moveFile(context);
        } else {
            moveDirectory(context);
        }
    }

    @Override
    public void rollback(MoveRollbackSnapshot snapshot) {
        if (snapshot.hasMovedObjects()) {
            snapshot.movedObjects().forEach(m -> {
                try {
                    storageService.moveObject(snapshot.userId(), m.pathTo(), m.pathFrom());
                } catch (Exception e) {
                    log.error("Failed to rollback move: {} -> {}", m.pathTo(), m.pathFrom(), e);
                }
            });
        }
    }

    private void moveFile(MoveContext context) {
        storageService.moveObject(context.getUserId(), context.getPathFrom(), context.getPathTo());
        context.addMovedObject(context.getPathFrom(), context.getPathTo());
    }

    private void moveDirectory(MoveContext context) {
        List<ResourceMetadataDto> files = metadataService.findFilesByPathPrefix(
                context.getUserId(), context.getPathFrom()
        );

        List<CompletableFuture<Void>> futures = files.stream()
                .map(f -> CompletableFuture.runAsync(() -> {
                    String newPath = replacePathPrefix(context.getPathFrom(), context.getPathTo(), f.path());
                    storageService.moveObject(context.getUserId(), f.path(), newPath);
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

    private String replacePathPrefix(String pathFrom, String pathTo, String filePath) {
        return pathTo + filePath.substring(pathFrom.length());
    }
}
