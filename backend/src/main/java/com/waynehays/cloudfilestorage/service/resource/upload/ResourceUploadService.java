package com.waynehays.cloudfilestorage.service.resource.upload;

import com.waynehays.cloudfilestorage.dto.internal.NewFileDto;
import com.waynehays.cloudfilestorage.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.ResourceStorageOperationException;
import com.waynehays.cloudfilestorage.mapper.NewFileMapper;
import com.waynehays.cloudfilestorage.mapper.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.quota.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.service.storage.ResourceStorageService;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceUploader implements ResourceUploaderApi {
    private final NewFileMapper newFileMapper;
    private final ResourceDtoMapper resourceDtoMapper;
    private final ExecutorService uploadExecutor;
    private final ResourceStorageService storageService;
    private final StorageQuotaServiceApi quotaService;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public List<ResourceDto> upload(Long userId, List<UploadObjectDto> objects) {
        log.info("Upload started: userId={}, objects count={}", userId, objects.size());

        validateUpload(userId, objects);
        long totalSize = calculateObjectsSize(objects);
        quotaService.reserveSpace(userId, totalSize);

        UploadContext context = new UploadContext();

        try {
            List<ResourceDto> uploadedFiles = uploadFiles(userId, objects, context);
            List<ResourceDto> createdDirectories = saveNewDirectories(userId, uploadedFiles, context);
            List<ResourceDto> result = new ArrayList<>(createdDirectories);
            result.addAll(uploadedFiles);

            log.info("Upload completed: userId={}, objects count={}, created directories={}",
                    userId, uploadedFiles.size(), createdDirectories.size());
            return result;
        } catch (Exception e) {
            log.warn("Upload failed for userId={}, initiating rollback", userId);
            rollback(userId, totalSize, context);
            log.warn("Rollback successful for userId={}", userId);
            throw e;
        }
    }

    private void validateUpload(Long userId, List<UploadObjectDto> objects) {
        List<String> paths = objects.stream()
                .map(UploadObjectDto::fullPath)
                .toList();

        Set<String> seen = new HashSet<>();
        Set<String> duplicates = new HashSet<>();

        for (String path : paths) {
            if (!seen.add(path)) {
                duplicates.add(path);
            }
        }

        if (!duplicates.isEmpty()) {
            throw new ResourceAlreadyExistsException("Duplicate paths in upload request", duplicates.stream().toList());
        }

        Set<String> existing = metadataService.findExistingPaths(userId, new HashSet<>(paths));

        if (!existing.isEmpty()) {
            throw new ResourceAlreadyExistsException("Resources already exist", new ArrayList<>(existing));
        }
    }

    private long calculateObjectsSize(List<UploadObjectDto> objects) {
        return objects.stream()
                .mapToLong(UploadObjectDto::size)
                .sum();
    }

    private List<ResourceDto> uploadFiles(Long userId, List<UploadObjectDto> objects, UploadContext context) {
        List<CompletableFuture<ResourceDto>> futures = objects.stream()
                .map(o -> CompletableFuture.supplyAsync(() -> {
                    storageService.putObject(userId, o);
                    context.addStoragePath(o.fullPath());
                    return resourceDtoMapper.fileFromPath(o.fullPath(), o.size());
                }, uploadExecutor))
                .toList();

        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        } catch (CompletionException e) {
            throw new ResourceStorageOperationException("Failed to upload some files to storage", e);
        }

        List<ResourceDto> result = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        List<NewFileDto> newFiles = newFileMapper.toNewFiles(objects);
        metadataService.saveFiles(userId, newFiles);
        objects.forEach(o -> context.addMetadataPath(o.fullPath()));
        return result;
    }

    private List<ResourceDto> saveNewDirectories(Long userId, List<ResourceDto> uploadedResources, UploadContext context) {
        Set<String> allDirectoriesPaths = uploadedResources.stream()
                .flatMap(r -> PathUtils.getAllDirectories(r.path()).stream())
                .map(PathUtils::ensureTrailingSlash)
                .collect(Collectors.toSet());

        if (allDirectoriesPaths.isEmpty()) {
            return List.of();
        }

        Set<String> existingPaths = metadataService.findExistingPaths(userId, allDirectoriesPaths);
        metadataService.saveDirectories(userId, allDirectoriesPaths);

        Set<String> newDirectoriesPaths = allDirectoriesPaths.stream()
                .filter(d -> !existingPaths.contains(d))
                .collect(Collectors.toSet());

        newDirectoriesPaths.forEach(context::addMetadataPath);
        return resourceDtoMapper.directoriesFromPaths(newDirectoriesPaths);
    }

    private void rollback(Long userId, long totalSize, UploadContext context) {
        safeExecuteRollback(
                () -> quotaService.releaseSpace(userId, totalSize),
                "Failed to release quota during rollback",
                userId);

        safeExecuteRollback(
                () -> {
                    if (context.containsStoragePaths()) {
                        storageService.deleteObjects(userId, context.getStoragePaths());
                    }
                },
                "Failed to rollback storage",
                userId);

        safeExecuteRollback(
                () -> {
                    if (context.containsMetadataPaths()) {
                        metadataService.deleteByPaths(userId, context.getMetadataPaths());
                    }
                },
                "Failed to rollback metadata",
                userId);

    }

    private void safeExecuteRollback(Runnable action, String errorMessage, Long userId) {
        try {
            action.run();
        } catch (Exception e) {
            log.error("{}: userId={}", errorMessage, userId, e);
        }
    }
}


