package com.waynehays.cloudfilestorage.service.directory;

import com.waynehays.cloudfilestorage.constant.Messages;
import com.waynehays.cloudfilestorage.component.converter.ResourceDtoConverterApi;
import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DirectoryService implements DirectoryServiceApi {
    private final ResourceStorageApi fileStorage;
    private final ResourceMetadataServiceApi metadataService;
    private final ResourceDtoConverterApi dtoConverter;
    private final StorageKeyResolverApi keyResolver;

    @Override
    public List<ResourceDto> getContent(Long userId, String directoryPath) {
        metadataService.findOrThrow(userId, directoryPath);

        return metadataService.findDirectChildren(userId, directoryPath)
                .stream()
                .map(dtoConverter::fromMetadata)
                .toList();
    }

    @Override
    public ResourceDto createDirectory(Long userId, String directoryPath) {
        if (metadataService.exists(userId, directoryPath)) {
            throw new ResourceAlreadyExistsException(Messages.ALREADY_EXISTS + directoryPath);
        }

        validateParentExists(userId, directoryPath);

        String storageKey = keyResolver.resolveKey(userId, directoryPath);
        fileStorage.createDirectory(storageKey);
        metadataService.saveDirectory(userId, directoryPath);

        return dtoConverter.directoryFromPath(directoryPath);
    }

    private void validateParentExists(Long userId, String directoryPath) {
        String pathToParent = PathUtils.extractParentPath(directoryPath);

        if (StringUtils.isNotEmpty(pathToParent)) {
            metadataService.findOrThrow(userId, pathToParent);
        }
    }
}
