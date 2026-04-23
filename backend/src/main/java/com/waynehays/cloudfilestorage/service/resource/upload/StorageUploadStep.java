package com.waynehays.cloudfilestorage.service.resource.upload;

import com.waynehays.cloudfilestorage.dto.internal.storage.UserPath;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.exception.ApplicationException;
import com.waynehays.cloudfilestorage.exception.ResourceStorageOperationException;
import com.waynehays.cloudfilestorage.mapper.ResourceDtoMapper;
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
class StorageUploadStep implements UploadStep{
    private final ResourceStorageServiceApi storageService;
    private final ResourceDtoMapper resourceDtoMapper;
    private final ExecutorService uploadExecutor;

    @Override
    public void execute(UploadContext context) {
        List<CompletableFuture<ResourceDto>> futures = context.getObjects().stream()
                .map(o -> CompletableFuture.supplyAsync(() -> {
                    storageService.putObject(context.getUserId(), o);
                    context.addUploadedToStoragePath(o.fullPath());
                    return resourceDtoMapper.fileFromPath(o.fullPath(), o.size());
                }, uploadExecutor))
                .toList();

        try {
            List<ResourceDto> uploaded = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
            context.addResult(uploaded);
        } catch (CompletionException e) {
            futures.forEach(f -> f.cancel(true));
            Throwable cause = e.getCause();
            if (cause instanceof ApplicationException ae) {
                throw ae;
            }
            throw new ResourceStorageOperationException("Failed to upload some files to storage", cause);
        }
    }

    @Override
    public void rollback(RollbackSnapshot snapshot) {
        if (snapshot.hasUploadedToStoragePaths()) {
            List<UserPath> userPaths = snapshot.uploadedToStoragePaths().stream()
                    .map(p -> new UserPath(snapshot.userId(), p))
                    .toList();
            storageService.deleteObjects(userPaths);
        }
    }
}
