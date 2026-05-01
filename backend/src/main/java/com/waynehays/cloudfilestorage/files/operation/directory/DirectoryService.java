package com.waynehays.cloudfilestorage.files.operation.directory;

import com.waynehays.cloudfilestorage.core.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.files.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.files.operation.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.infrastructure.path.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
class DirectoryService implements DirectoryServiceApi {
    private final ResourceDtoMapper mapper;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public List<ResourceDto> getContent(Long userId, String path) {
        List<ResourceMetadataDto> content = metadataService.findDirectoryContent(userId, path);
        return mapper.fromResourceMetadataDto(content);
    }

    @Override
    public ResourceDto createDirectory(Long userId, String path) {
        validate(userId, path);
        metadataService.saveDirectory(userId, path);

        log.info("Created directory: {}", path);
        return mapper.directoryFromPath(path);
    }

    private void validate(Long userId, String path) {
        String parentPath = PathUtils.extractParentPath(path);

        if (!parentPath.isEmpty() && !metadataService.existsByPath(userId, parentPath)) {
            throw new ResourceNotFoundException("Directory not found", parentPath);
        }

        metadataService.throwIfAnyConflictingTypeExists(userId, List.of(path));
    }
}
