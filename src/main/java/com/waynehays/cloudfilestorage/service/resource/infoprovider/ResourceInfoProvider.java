package com.waynehays.cloudfilestorage.service.resource.infoprovider;

import com.waynehays.cloudfilestorage.constants.Messages;
import com.waynehays.cloudfilestorage.component.converter.ResourceDtoConverterApi;
import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.filestorage.FileStorageApi;
import com.waynehays.cloudfilestorage.filestorage.dto.MetaData;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResourceInfoProvider implements ResourceInfoProviderApi {
    private final FileStorageApi fileStorage;
    private final StorageKeyResolverApi keyResolver;
    private final ResourceDtoConverterApi dtoConverter;

    @Override
    public ResourceDto getInfo(Long userId, String path) {
        String objectKey = keyResolver.resolveKey(userId, path);

        MetaData metaData = fileStorage.getMetaData(objectKey)
                .orElseThrow(() -> new ResourceNotFoundException(Messages.NOT_FOUND + path));

        if (PathUtils.isDirectory(path)) {
            return dtoConverter.directoryFromPath(path);
        }

        return dtoConverter.convert(metaData, path);
    }
}
