package com.waynehays.cloudfilestorage.service.resource.uploader;

import com.waynehays.cloudfilestorage.component.converter.ResourceDtoConverterApi;
import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import com.waynehays.cloudfilestorage.dto.FileData;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.exception.FileStorageException;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceUploader implements ResourceUploaderApi {
    private static final String MSG_LIST_ALREADY_EXISTS = "Resources are already exist by paths: ";
    private static final String MSG_FAILED_PROCESS_STREAM = "Failed to process file stream";
    private static final String MSG_DUPLICATE_FILES = "Duplicate files in request";
    private static final String LOG_FAILED_ROLLBACK = "Failed to rollback key: {}";

    private final ResourceStorageApi fileStorage;
    private final StorageKeyResolverApi keyResolver;
    private final ResourceDtoConverterApi dtoConverter;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public List<ResourceDto> upload(Long userId, List<FileData> files) {
        checkForDuplicates(userId, files);
        UploadContext context = new UploadContext();

        try {
            List<ResourceDto> uploadedFiles = uploadFiles(userId, files, context);
            List<ResourceDto> createdDirectories = createDirectories(userId, uploadedFiles, context);
            List<ResourceDto> result = new ArrayList<>(createdDirectories);
            result.addAll(uploadedFiles);
            return result;
        } catch (Exception e) {
            rollback(userId, context);
            throw e;
        }
    }

    private void checkForDuplicates(Long userId, List<FileData> files) {
        List<String> paths = files.stream()
                .map(FileData::fullPath)
                .toList();
        Set<String> uniquePaths = new HashSet<>(paths);

        if (uniquePaths.size() != paths.size()) {
            throw new ResourceAlreadyExistsException(MSG_DUPLICATE_FILES);
        }

        List<String> existingPaths = paths.stream()
                .filter(path -> metadataService.exists(userId, path))
                .toList();

        if (!existingPaths.isEmpty()) {
            throw new ResourceAlreadyExistsException(MSG_LIST_ALREADY_EXISTS + existingPaths);
        }
    }

    private List<ResourceDto> uploadFiles(Long userId, List<FileData> files, UploadContext context) {
        List<ResourceDto> result = new ArrayList<>();

        for (FileData file : files) {
            String storageKey = keyResolver.resolveKey(userId, file.fullPath());
            putObjectOrThrow(file, storageKey);
            context.addStorageKey(storageKey);

            metadataService.saveFile(userId, file.fullPath(), file.size());
            context.addMetadataPath(file.fullPath());

            ResourceDto dto = dtoConverter.fileFromPath(file.fullPath(), file.size());
            result.add(dto);
        }

        return result;
    }

    private void putObjectOrThrow(FileData fileData, String storageKey) {
        try (InputStream inputStream = fileData.inputStreamSupplier().get()) {
            fileStorage.putObject(inputStream, storageKey, fileData.size(), fileData.contentType());
        } catch (IOException e) {
            throw new FileStorageException(MSG_FAILED_PROCESS_STREAM, e);
        }
    }

    private List<ResourceDto> createDirectories(Long userId, List<ResourceDto> uploadedFiles, UploadContext context) {
        Set<String> uniqueDirectories = uploadedFiles.stream()
                .flatMap(file -> PathUtils.getAllDirectories(file.path()).stream())
                .map(PathUtils::ensureTrailingSeparator)
                .collect(Collectors.toSet());

        List<ResourceDto> result = new ArrayList<>();

        for (String directory : uniqueDirectories) {
            if (!metadataService.exists(userId, directory)) {
                String objectKey = keyResolver.resolveKey(userId, directory);
                fileStorage.createDirectory(objectKey);
                context.addStorageKey(objectKey);

                metadataService.saveDirectory(userId, directory);
                context.addMetadataPath(directory);

                result.add(dtoConverter.directoryFromPath(directory));
            }
        }

        return result;
    }

    private void rollback(Long userId, UploadContext context) {
        context.getStorageKeys()
                .forEach(key -> tryDelete(() -> fileStorage.delete(key), key));
        context.getMetadataPaths()
                .forEach(path -> tryDelete(() -> metadataService.delete(userId, path), path));
    }

    private void tryDelete(Runnable action, String identifier) {
        try {
            action.run();
        } catch (Exception e) {
            log.warn(LOG_FAILED_ROLLBACK, identifier, e);
        }
    }
}


