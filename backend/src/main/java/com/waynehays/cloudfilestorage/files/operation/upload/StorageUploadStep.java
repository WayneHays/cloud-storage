package com.waynehays.cloudfilestorage.files.operation.upload;

import com.waynehays.cloudfilestorage.files.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.files.operation.AsyncUtils;
import com.waynehays.cloudfilestorage.files.operation.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.infrastructure.storage.ResourceStorageServiceApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Component
@RequiredArgsConstructor
class StorageUploadStep implements UploadStep {
    private final ResourceStorageServiceApi storageService;
    private final ResourceDtoMapper resourceDtoMapper;
    private final ExecutorService uploadExecutor;

    @Override
    public void execute(UploadContext context) {
        List<CompletableFuture<Void>> futures = context.getObjects().stream()
                .map(o -> CompletableFuture.runAsync(() -> {
                    storageService.putObject(context.getUserId(), o.fullPath(), o.size(), o.contentType(), o.inputStreamSupplier());
                    context.addUploadedToStoragePath(o.fullPath());
                    ResourceDto result = resourceDtoMapper.fileFromPath(o.fullPath(), o.size());
                    context.addResult(result);
                }, uploadExecutor))
                .toList();

        AsyncUtils.joinAll(futures, "Failed to upload some files to storage");
    }

    @Override
    public void rollback(UploadRollbackDto snapshot) {
        storageService.deleteObjects(Map.of(snapshot.userId(), snapshot.uploadedToStoragePaths()));
    }

    @Override
    public boolean requiresRollback(UploadRollbackDto snapshot) {
        return snapshot.hasUploadedToStoragePaths();
    }
}
