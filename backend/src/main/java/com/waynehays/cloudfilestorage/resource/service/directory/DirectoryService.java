package com.waynehays.cloudfilestorage.resource.service.directory;

import com.waynehays.cloudfilestorage.resource.dto.internal.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.resource.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.resource.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.shared.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.resource.mapper.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.shared.utils.PathUtils;
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
