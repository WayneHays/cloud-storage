package com.waynehays.cloudfilestorage.files.operation.upload;

import com.waynehays.cloudfilestorage.files.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.files.operation.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.infrastructure.errorhandling.ApplicationException;
import com.waynehays.cloudfilestorage.infrastructure.storage.ResourceStorageException;
import com.waynehays.cloudfilestorage.infrastructure.storage.ResourceStorageServiceApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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
                    storageService.putObject(context.getUserId(), o.storageKey(), o.size(), o.contentType(), o.inputStreamSupplier());
                    context.addUploadedStorageKey(o.storageKey());
                    ResourceDto result = resourceDtoMapper.fileFromPath(o.fullPath(), o.size());
                    context.addResult(result);
                }, uploadExecutor))
                .toList();

        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        } catch (CompletionException e) {
            futures.forEach(f -> f.cancel(true));
            Throwable cause = e.getCause();

            if (cause instanceof ApplicationException ae) {
                throw ae;
            }
            throw new ResourceStorageException("Failed to upload some files to storage", cause);
        }
    }

    @Override
    public void rollback(UploadRollbackDto rollbackDto) {
        storageService.deleteObjects(Map.of(rollbackDto.userId(), rollbackDto.uploadedStorageKeys()));
    }

    @Override
    public boolean requiresRollback(UploadRollbackDto rollbackDto) {
        return rollbackDto.hasUploadedStorageKeys();
    }
}
