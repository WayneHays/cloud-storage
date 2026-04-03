package com.waynehays.cloudfilestorage.service.resource.uploader;

import com.waynehays.cloudfilestorage.mapper.NewFileMapper;
import com.waynehays.cloudfilestorage.mapper.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.service.storagequota.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageKeyResolverApi;
import com.waynehays.cloudfilestorage.component.validator.UploadValidator;
import com.waynehays.cloudfilestorage.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.exception.ResourceStorageOperationException;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
    private final UploadValidator uploadValidator;
    private final ResourceStorageApi resourceStorage;
    private final StorageQuotaServiceApi quotaService;
    private final ResourceStorageKeyResolverApi keyResolver;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public List<ResourceDto> upload(Long userId, List<UploadObjectDto> objects) {
        log.info("Upload started: userId={}, objects count={}", userId, objects.size());

        uploadValidator.validate(userId, objects);
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

    private long calculateObjectsSize(List<UploadObjectDto> objects) {
        return objects.stream()
                .mapToLong(UploadObjectDto::size)
                .sum();
    }

    private List<ResourceDto> uploadFiles(Long userId, List<UploadObjectDto> objects, UploadContext context) {
        List<CompletableFuture<ResourceDto>> futures = objects.stream()
                .map(o -> CompletableFuture.supplyAsync(() -> {
                    String storageKey = keyResolver.resolveKey(userId, o.fullPath());
                    putObjectOrThrow(o, storageKey);
                    context.addStorageKey(storageKey);
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

        metadataService.saveFiles(userId, newFileMapper.toNewFiles(objects));
        objects.forEach(o -> context.addMetadataPath(o.fullPath()));
        return result;
    }

    private void putObjectOrThrow(UploadObjectDto object, String storageKey) {
        try (InputStream inputStream = object.inputStreamSupplier().get()) {
            resourceStorage.putObject(inputStream, storageKey, object.size(), object.contentType());
        } catch (IOException e) {
            throw new ResourceStorageOperationException("Failed to process file stream");
        }
    }

    private List<ResourceDto> createDirectories(Long userId, List<ResourceDto> uploadedResources, UploadContext context) {
        Set<String> allDirectories = uploadedResources.stream()
                .flatMap(r -> PathUtils.getAllDirectories(r.path()).stream())
                .map(PathUtils::ensureTrailingSlash)
                .collect(Collectors.toSet());

        Set<String> existingPaths = metadataService.findExistingPaths(userId, allDirectories);

        Set<String> newDirectories = allDirectories.stream()
                .filter(d -> !existingPaths.contains(d))
                .collect(Collectors.toSet());

        newDirectories.forEach(context::addMetadataPath);

        return newDirectories.stream()
                .map(resourceDtoMapper::directoryFromPath)
                .toList();
    }

    private void rollback(Long userId, long totalSize, UploadContext context) {
        try {
            quotaService.releaseSpace(userId, totalSize);
        } catch (Exception e) {
            log.error("Failed to release quota during rollback: userId={}", userId, e);
        }

        try {
            if (!context.getStorageKeys().isEmpty()) {
                resourceStorage.deleteList(context.getStorageKeys());
            }
        } catch (Exception e) {
            log.error("Failed to rollback storage keys: userId={}", userId, e);
        }

        try {
            if (!context.getMetadataPaths().isEmpty()) {
                metadataService.deleteByPaths(userId, context.getMetadataPaths());
            }
        } catch (Exception e) {
            log.error("Failed to rollback metadata: userId={}", userId, e);
        }
    }
}


