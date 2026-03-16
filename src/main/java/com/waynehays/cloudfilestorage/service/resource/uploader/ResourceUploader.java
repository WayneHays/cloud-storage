package com.waynehays.cloudfilestorage.service.resource.uploader;

import com.waynehays.cloudfilestorage.component.converter.ResourceDtoConverterApi;
import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import com.waynehays.cloudfilestorage.dto.FileData;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.exception.FileStorageException;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.filestorage.FileStorageApi;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceUploader implements ResourceUploaderApi {
    private static final String LOG_FAILED_ROLLBACK = "Failed to rollback key: {}";
    private static final String MSG_LIST_ALREADY_EXISTS = "Resources are already exist by paths: ";
    private static final String MSG_FAILED_PROCESS_STREAM = "Failed to process file stream";
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final FileStorageApi fileStorage;
    private final StorageKeyResolverApi keyResolver;
    private final ResourceDtoConverterApi dtoConverter;

    @Override
    public List<ResourceDto> upload(Long userId, List<FileData> files) {
        checkForDuplicates(userId, files);
        List<String> uploadedKeys = new ArrayList<>();
        List<String> createdDirectoryKeys = new ArrayList<>();

        try {
            List<ResourceDto> uploadedFiles = uploadFiles(userId, files, uploadedKeys);
            List<ResourceDto> createdDirectories = collectDirectories(userId, uploadedFiles, createdDirectoryKeys);
            return Stream.concat(createdDirectories.stream(), uploadedFiles.stream()).toList();
        } catch (Exception e) {
            rollback(uploadedKeys, createdDirectoryKeys);
            throw e;
        }
    }

    private void checkForDuplicates(Long userId, List<FileData> files) {
        List<String> duplicateFilesPaths = files.stream()
                .map(FileData::fullPath)
                .filter(path -> {
                    String storageKey = keyResolver.resolveKey(userId, path);
                    return fileStorage.exists(storageKey);
                })
                .toList();

        if (ObjectUtils.isNotEmpty(duplicateFilesPaths)) {
            throw new ResourceAlreadyExistsException(MSG_LIST_ALREADY_EXISTS + duplicateFilesPaths);
        }
    }

    private List<ResourceDto> uploadFiles(Long userId, List<FileData> files, List<String> uploadedKeys) {
        List<ResourceDto> result = new ArrayList<>();

        for (FileData fileData : files) {
            String storageKey = keyResolver.resolveKey(userId, fileData.fullPath());
            putObjectOrThrow(fileData, storageKey);
            uploadedKeys.add(storageKey);
            result.add(dtoConverter.fileFromPath(fileData.fullPath(), fileData.size()));
        }

        return result;
    }

    private void putObjectOrThrow(FileData fileData, String storageKey) {
        try (InputStream inputStream = fileData.inputStreamSupplier().get()) {
            String contentType = resolveContentType(fileData.contentType());
            fileStorage.putObject(inputStream, storageKey, fileData.size(), contentType);
        } catch (IOException e) {
            throw new FileStorageException(MSG_FAILED_PROCESS_STREAM, e);
        }
    }

    private List<ResourceDto> collectDirectories(Long userId, List<ResourceDto> uploadedFiles, List<String> createdDirectoryKeys) {
        Set<String> uniqueDirectories = uploadedFiles.stream()
                .flatMap(file -> PathUtils.getAllDirectories(file.path()).stream())
                .collect(Collectors.toSet());

        List<ResourceDto> createdDirectories = new ArrayList<>();

        for (String directory : uniqueDirectories) {
            String objectKey = keyResolver.resolveKeyToDirectory(userId, directory);

            if (!fileStorage.exists(objectKey)) {
                fileStorage.createDirectory(objectKey);
                createdDirectoryKeys.add(objectKey);
                createdDirectories.add(dtoConverter.directoryFromPath(directory));
            }
        }

        return createdDirectories;
    }

    private void rollback(List<String> uploadedKeys, List<String> createdDirectoryKeys) {
        createdDirectoryKeys.forEach(this::tryDelete);
        uploadedKeys.forEach(this::tryDelete);
    }

    private void tryDelete(String key) {
        try {
            fileStorage.delete(key);
        } catch (Exception e) {
            log.warn(LOG_FAILED_ROLLBACK, key, e);
        }
    }

    private String resolveContentType(String contentType) {
        return contentType != null ? contentType : DEFAULT_CONTENT_TYPE;
    }
}


