package com.waynehays.cloudfilestorage.service.directory;

import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.mapper.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectoryService implements DirectoryServiceApi {
    private final ResourceDtoMapper mapper;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public List<ResourceDto> getContent(Long userId, String path) {
        return metadataService.findDirectChildren(userId, path)
                .stream()
                .map(mapper::fromResourceMetadataDto)
                .toList();
    }

    @Override
    public ResourceDto createDirectory(Long userId, String path) {
        log.info("Start creating directory: userId={}, path={}", userId, path);

        metadataService.validateDirectoryCreation(userId, path);
        metadataService.saveDirectory(userId, path);

        log.info("Successfully created directory: userId={}, path={}", userId, path);
        return mapper.directoryFromPath(path);
    }
}
