package com.waynehays.cloudfilestorage.files.operation.upload.step;

import com.waynehays.cloudfilestorage.core.exception.ApplicationException;
import com.waynehays.cloudfilestorage.files.api.support.ResourceResponseMapper;
import com.waynehays.cloudfilestorage.files.operation.upload.dto.Context;
import com.waynehays.cloudfilestorage.files.operation.upload.dto.RollbackDto;
import com.waynehays.cloudfilestorage.infrastructure.storage.ResourceStorageServiceApi;
import com.waynehays.cloudfilestorage.infrastructure.storage.exception.ResourceStorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

@Component
@RequiredArgsConstructor
public class StorageUploadStep implements UploadStep {
    private final ExecutorService uploadExecutor;
    private final ResourceStorageServiceApi storageService;
    private final ResourceResponseMapper responseMapper;

    @Override
    public void execute(Context context) {
        List<CompletableFuture<Void>> futures = context.getObjects().stream()
                .map(o -> CompletableFuture.runAsync(() -> {
                    storageService.putObject(context.getUserId(), o.storageKey(), o.size(), o.contentType(), o.inputStreamSource());
                    context.addUploadedStorageKey(o.storageKey());
                    context.addToResult(responseMapper.fromUploadObjectDto(o));
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
    public void rollback(RollbackDto rollbackDto) {
        storageService.deleteObjects(Map.of(rollbackDto.userId(), rollbackDto.uploadedStorageKeys()));
    }

    @Override
    public boolean requiresRollback(RollbackDto rollbackDto) {
        return rollbackDto.hasUploadedStorageKeys();
    }
}
