package com.waynehays.cloudfilestorage.service.resource.searcher;

import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.filestorage.FileStorageApi;
import com.waynehays.cloudfilestorage.component.converter.ResourceDtoConverterApi;
import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceSearcher implements ResourceSearcherApi {
    private final FileStorageApi fileStorage;
    private final StorageKeyResolverApi keyResolver;
    private final ResourceDtoConverterApi dtoConverter;

    @Override
    public List<ResourceDto> search(Long userId, String query) {
        String prefixToRoot = keyResolver.resolveKeyToRoot(userId);
        String lowerQuery = query.toLowerCase();

        return fileStorage.getListRecursive(prefixToRoot)
                .stream()
                .filter(metaData -> metaData.name().toLowerCase().contains(lowerQuery))
                .map(metaData -> {
                    String path = keyResolver.extractPath(userId, metaData.key());
                    return dtoConverter.convert(metaData, path);
                })
                .toList();
    }
}
