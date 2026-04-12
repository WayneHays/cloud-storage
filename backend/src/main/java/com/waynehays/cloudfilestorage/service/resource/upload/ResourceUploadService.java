package com.waynehays.cloudfilestorage.service.resource.upload;

import com.waynehays.cloudfilestorage.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.dto.internal.metadata.DirectoryRow;
import com.waynehays.cloudfilestorage.dto.internal.metadata.FileRow;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.exception.ApplicationException;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.ResourceStorageOperationException;
import com.waynehays.cloudfilestorage.mapper.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.mapper.ResourceRowMapper;
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
public class ResourceUploadService implements ResourceUploadServiceApi {
    private final ResourceRowMapper resourceRowMapper;
    private final ResourceDtoMapper resourceDtoMapper;
    private final ExecutorService uploadExecutor;
    private final ResourceStorageService storageService;
    private final StorageQuotaServiceApi quotaService;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public List<ResourceDto> upload(Long userId, List<UploadObjectDto> objects) {
        log.info("Upload started: userId={}, objects count={}", userId, objects.size());

        validateUpload(userId, objects);
        long totalSize = calculateSize(objects);
        UploadContext context = new UploadContext();

        try {
            quotaService.reserveSpace(userId, totalSize);
            context.markQuotaReserved();
            List<ResourceDto> uploadedFiles = uploadFilesAndSaveMetadata(userId, objects, context);
            List<ResourceDto> createdDirectories = createMissingDirectories(userId, uploadedFiles, context);
            List<ResourceDto> result = new ArrayList<>(createdDirectories);
            result.addAll(uploadedFiles);

            log.info("Upload completed: userId={}, objects count={}, created directories={}",
                    userId, uploadedFiles.size(), createdDirectories.size());
            return result;
        } catch (Exception e) {
            log.warn("Upload failed for userId={}, initiating rollback", userId);
            rollback(userId, totalSize, context);
            throw e;
        }
    }

    private void validateUpload(Long userId, List<UploadObjectDto> objects) {
        List<String> paths = objects.stream()
                .map(UploadObjectDto::fullPath)
                .toList();

        Set<String> seen = new HashSet<>();
        List<String> duplicates = paths.stream()
                .filter(p -> !seen.add(p))
                .toList();

        if (!duplicates.isEmpty()) {
            throw new ResourceAlreadyExistsException("Duplicate paths in upload request", duplicates.stream().toList());
        }

        Set<String> existing = metadataService.findExistingPaths(userId, new HashSet<>(paths));

        if (!existing.isEmpty()) {
            throw new ResourceAlreadyExistsException("Resources already exist", new ArrayList<>(existing));
        }
    }

    private long calculateSize(List<UploadObjectDto> objects) {
        return objects.stream()
                .mapToLong(UploadObjectDto::size)
                .sum();
    }

    private List<ResourceDto> uploadFilesAndSaveMetadata(Long userId, List<UploadObjectDto> objects, UploadContext context) {
        List<ResourceDto> result = uploadToStorage(userId, objects, context);
        saveFilesMetadata(userId, objects, context);
        return result;
    }

    private List<ResourceDto> uploadToStorage(Long userId, List<UploadObjectDto> objects, UploadContext context) {
        List<CompletableFuture<ResourceDto>> futures = objects.stream()
                .map(o -> CompletableFuture.supplyAsync(() -> {
                    storageService.putObject(userId, o);
                    context.addUploadedToStoragePath(o.fullPath());
                    return resourceDtoMapper.fileFromPath(o.fullPath(), o.size());
                }, uploadExecutor))
                .toList();

        try {
            return futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ApplicationException ae) {
                throw ae;
            }
            throw new ResourceStorageOperationException("Failed to upload some files to storage", cause);
        }
    }

    private void saveFilesMetadata(Long userId, List<UploadObjectDto> objects, UploadContext context) {
        List<FileRow> newFiles = resourceRowMapper.toFileRows(objects);
        metadataService.saveFiles(userId, newFiles);
        objects.forEach(o -> context.addSavedToDatabasePath(o.fullPath()));
    }

    private List<ResourceDto> createMissingDirectories(Long userId, List<ResourceDto> uploadedResources, UploadContext context) {
        Set<String> allDirectoriesPaths = collectDirectoryPaths(uploadedResources);

        if (allDirectoriesPaths.isEmpty()) {
            return List.of();
        }

        Set<String> missingPaths = metadataService.findMissingPaths(userId, allDirectoriesPaths);

        if (missingPaths.isEmpty()) {
            return List.of();
        }

        List<DirectoryRow> directoriesToSave = resourceRowMapper.toDirectoryRows(missingPaths);
        metadataService.saveDirectories(userId, directoriesToSave);

        missingPaths.forEach(context::addSavedToDatabasePath);
        return resourceDtoMapper.directoriesFromPaths(missingPaths);
    }

    private Set<String> collectDirectoryPaths(List<ResourceDto> resources) {
        return resources.stream()
                .flatMap(r -> PathUtils.getAllDirectories(r.path()).stream())
                .map(PathUtils::ensureTrailingSlash)
                .collect(Collectors.toSet());
    }

    private void rollback(Long userId, long totalSize, UploadContext context) {
        safeExecuteRollback(
                () -> {
                    if (context.hasAnySavedToDbPaths()) {
                        metadataService.deleteByPaths(userId, context.getSavedToDbPaths());
                    }
                },
                "Failed to rollback metadata",
                userId);

        safeExecuteRollback(
                () -> {
                    if (context.hasAnyUploadedToStoragePaths()) {
                        storageService.deleteObjects(userId, context.getUploadedToStoragePaths());
                    }
                },
                "Failed to rollback storage",
                userId);

        safeExecuteRollback(
                () -> {
                    if (context.isQuotaReserved()) {
                        quotaService.releaseSpace(userId, totalSize);
                    }
                },
                "Failed to release quota during rollback",
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


