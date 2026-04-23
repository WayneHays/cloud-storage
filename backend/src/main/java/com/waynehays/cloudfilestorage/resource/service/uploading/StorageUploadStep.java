package com.waynehays.cloudfilestorage.resource.service.uploading;

import com.waynehays.cloudfilestorage.shared.utils.AsyncUtils;
import com.waynehays.cloudfilestorage.storage.dto.UserPath;
import com.waynehays.cloudfilestorage.resource.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.resource.mapper.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.storage.service.ResourceStorageServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
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
                    storageService.putObject(context.getUserId(), o);
                    context.addUploadedToStoragePath(o.fullPath());
                    ResourceDto result = resourceDtoMapper.fileFromPath(o.fullPath(), o.size());
                    context.addResult(result);
                }, uploadExecutor))
                .toList();

        AsyncUtils.joinAll(futures, "Failed to upload some files to storage");
    }

    @Override
    public void rollback(UploadRollbackSnapshot snapshot) {
        List<UserPath> userPaths = snapshot.uploadedToStoragePaths().stream()
                .map(p -> new UserPath(snapshot.userId(), p))
                .toList();
        storageService.deleteObjects(userPaths);
    }

    @Override
    public boolean requiresRollback(UploadRollbackSnapshot snapshot) {
        return snapshot.hasUploadedToStoragePaths();
    }
}
