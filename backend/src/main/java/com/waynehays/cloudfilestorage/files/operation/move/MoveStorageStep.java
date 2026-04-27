package com.waynehays.cloudfilestorage.files.operation.move;

import com.waynehays.cloudfilestorage.core.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.files.operation.AsyncUtils;
import com.waynehays.cloudfilestorage.infrastructure.storage.ResourceStorageServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
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
        if (context.isMovingFile()) {
            moveFile(context);
        } else {
            moveDirectory(context);
        }
    }

    @Override
    public void rollback(MoveRollbackDto snapshot) {
        snapshot.movedObjects()
                .forEach((mo -> {
                    try {
                        storageService.moveObject(snapshot.userId(), mo.pathTo(), mo.pathFrom());
                    } catch (Exception e) {
                        log.error("Failed to rollback move: {} -> {}", mo.pathTo(), mo.pathFrom(), e);
                    }
                }));
    }

    @Override
    public boolean requiresRollback(MoveRollbackDto snapshot) {
        return snapshot.hasMovedObjects();
    }

    private void moveFile(MoveContext context) {
        storageService.moveObject(context.getUserId(), context.getPathFrom(), context.getPathTo());
        context.addMovedObject(context.getPathFrom(), context.getPathTo());
    }

    private void moveDirectory(MoveContext context) {
        List<ResourceMetadataDto> files = metadataService.findFilesByPathPrefix(
                context.getUserId(),
                context.getPathFrom());

        List<CompletableFuture<Void>> futures = files.stream()
                .map(f -> CompletableFuture.runAsync(() -> {
                    String newPath = replacePathPrefix(context.getPathFrom(), context.getPathTo(), f.path());
                    storageService.moveObject(context.getUserId(), f.path(), newPath);
                    context.addMovedObject(f.path(), newPath);
                }, moveExecutor))
                .toList();

        AsyncUtils.joinAll(futures, "Failed to move some files in storage");
    }

    private String replacePathPrefix(String pathFrom, String pathTo, String filePath) {
        return pathTo + filePath.substring(pathFrom.length());
    }
}
