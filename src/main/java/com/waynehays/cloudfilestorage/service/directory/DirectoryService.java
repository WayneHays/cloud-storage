package com.waynehays.cloudfilestorage.service.directory;

import com.waynehays.cloudfilestorage.component.converter.ResourceDtoConverterApi;
import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectoryService implements DirectoryServiceApi {
    private final ResourceStorageApi fileStorage;
    private final ResourceMetadataServiceApi metadataService;
    private final ResourceDtoConverterApi dtoConverter;
    private final StorageKeyResolverApi keyResolver;

    @Override
    public List<ResourceDto> getContent(Long userId, String directoryPath) {
        return metadataService.findDirectChildren(userId, directoryPath)
                .stream()
                .map(dtoConverter::fromMetadata)
                .toList();
    }

    @Override
    public ResourceDto createDirectory(Long userId, String directoryPath) {
        log.info("Start create directory: userId={}, path={}", userId, directoryPath);

        metadataService.throwIfExists(userId, directoryPath);
        metadataService.ensureParentExists(userId, directoryPath);

        String storageKey = keyResolver.resolveKey(userId, directoryPath);
        fileStorage.createDirectory(storageKey);
        metadataService.saveDirectory(userId, directoryPath);

        log.info("Successfully created directory: userId={}, path={}", userId, directoryPath);
        return dtoConverter.directoryFromPath(directoryPath);
    }
}
