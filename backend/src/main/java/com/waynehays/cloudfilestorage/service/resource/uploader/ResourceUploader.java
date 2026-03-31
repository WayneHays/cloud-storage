package com.waynehays.cloudfilestorage.service.resource.uploader;

import com.waynehays.cloudfilestorage.component.ResourceDtoConverter;
import com.waynehays.cloudfilestorage.service.storagequota.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageKeyResolverApi;
import com.waynehays.cloudfilestorage.component.validator.UploadValidator;
import com.waynehays.cloudfilestorage.dto.ObjectData;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceUploader implements ResourceUploaderApi {
    private final UploadValidator uploadValidator;
    private final ResourceDtoConverter dtoConverter;
    private final ResourceStorageApi resourceStorage;
    private final StorageQuotaServiceApi quotaService;
    private final ResourceStorageKeyResolverApi keyResolver;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public List<ResourceDto> upload(Long userId, List<ObjectData> objects) {
        log.info("Upload started: userId={}, objects count={}", userId, objects.size());
        uploadValidator.validate(userId, objects);

        long totalSize = objects.stream()
                .mapToLong(ObjectData::size)
                .sum();
        quotaService.reserveSpace(userId, totalSize);

        UploadContext context = new UploadContext();

        try {
            List<ResourceDto> uploadedFiles = uploadFiles(userId, objects, context);
            List<ResourceDto> createdDirectories = createDirectories(userId, uploadedFiles, context);
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

    private List<ResourceDto> uploadFiles(Long userId, List<ObjectData> objects, UploadContext context) {
        List<ResourceDto> result = new ArrayList<>();

        for (ObjectData object : objects) {
            String storageKey = keyResolver.resolveKey(userId, object.fullPath());
            putObjectOrThrow(object, storageKey);
            context.addStorageKey(storageKey);

            metadataService.saveFile(userId, object.fullPath(), object.size());
            context.addMetadataPath(object.fullPath());

            ResourceDto dto = dtoConverter.fileFromPath(object.fullPath(), object.size());
            result.add(dto);
        }

        return result;
    }

    private void putObjectOrThrow(ObjectData object, String storageKey) {
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

        if (newDirectories.isEmpty()) {
            return List.of();
        }

        metadataService.saveDirectories(userId, newDirectories);
        newDirectories.forEach(context::addMetadataPath);

        return newDirectories.stream()
                .map(dtoConverter::directoryFromPath)
                .toList();
    }

    private void rollback(Long userId, long totalSize, UploadContext context) {
        quotaService.releaseSpace(userId, totalSize);
        context.getStorageKeys()
                .forEach(key -> tryDelete(() -> resourceStorage.deleteObject(key), key));
        context.getMetadataPaths()
                .forEach(path -> tryDelete(() -> metadataService.delete(userId, path), path));
    }

    private void tryDelete(Runnable action, String identifier) {
        try {
            action.run();
        } catch (Exception e) {
            log.warn("Failed to rollback key: {}", identifier, e);
        }
    }
}


