package com.waynehays.cloudfilestorage.service.directory;

import com.waynehays.cloudfilestorage.constants.Messages;
import com.waynehays.cloudfilestorage.component.converter.ResourceDtoConverterApi;
import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.filestorage.FileStorageApi;
import com.waynehays.cloudfilestorage.filestorage.dto.MetaData;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DirectoryService implements DirectoryServiceApi {
    private final FileStorageApi fileStorage;
    private final ResourceDtoConverterApi dtoConverter;
    private final StorageKeyResolverApi keyResolver;

    @Override
    public List<ResourceDto> getContent(Long userId, String directoryPath) {
        String prefix = keyResolver.resolveKey(userId, directoryPath);
        List<MetaData> content = fileStorage.getList(prefix);

        if (content.isEmpty()) {
            fileStorage.getMetaData(prefix)
                    .orElseThrow(() -> new ResourceNotFoundException(Messages.NOT_FOUND + directoryPath));
        }

        return content.stream()
                .filter(metaData -> !metaData.key().equals(prefix))
                .map(metaData -> {
                    String path = keyResolver.extractPath(userId, metaData.key());
                    return dtoConverter.convert(metaData, path);
                })
                .toList();
    }

    @Override
    public ResourceDto createDirectory(Long userId, String directoryPath) {
        String storageKey = keyResolver.resolveKey(userId, directoryPath);

        if (fileStorage.exists(storageKey)) {
            throw new ResourceAlreadyExistsException(Messages.ALREADY_EXISTS + directoryPath);
        }

        validateParentExists(userId, directoryPath);

        fileStorage.createDirectory(storageKey);
        return dtoConverter.directoryFromPath(directoryPath);
    }

    private void validateParentExists(Long userId, String directoryPath) {
        String pathToParent = PathUtils.extractParentPath(directoryPath);

        if (StringUtils.isNotEmpty(pathToParent)) {
            String storageKeyToParent = keyResolver.resolveKey(userId, pathToParent);
            fileStorage.getMetaData(storageKeyToParent)
                    .orElseThrow(() -> new ResourceNotFoundException(Messages.NOT_FOUND + pathToParent));
        }
    }
}
