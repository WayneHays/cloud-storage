package com.waynehays.cloudfilestorage.service.directory;

import com.waynehays.cloudfilestorage.dto.internal.metadata.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.mapper.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectoryService implements DirectoryServiceApi {
    private final ResourceDtoMapper mapper;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public List<ResourceDto> getContent(Long userId, String path) {
        List<ResourceMetadataDto> content = metadataService.findDirectoryContent(userId, path);
        return mapper.fromResourceMetadataDto(content);
    }

    @Override
    public ResourceDto createDirectory(Long userId, String path) {
        validateParentExists(userId, path);
        metadataService.saveDirectory(userId, path);
        return mapper.directoryFromPath(path);
    }

    private void validateParentExists(Long userId, String path) {
        String parentPath = PathUtils.extractParentPath(path);

        if (!parentPath.isEmpty() && !metadataService.existsByPath(userId, parentPath)) {
            throw new ResourceNotFoundException("Directory not found", parentPath);
        }
    }
}
