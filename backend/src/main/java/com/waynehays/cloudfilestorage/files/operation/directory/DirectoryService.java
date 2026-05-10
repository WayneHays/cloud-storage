package com.waynehays.cloudfilestorage.files.operation.directory;

import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.core.metadata.service.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.files.api.dto.response.ResourceResponse;
import com.waynehays.cloudfilestorage.files.api.support.ResourceResponseMapper;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
class DirectoryService implements DirectoryServiceApi {
    private final ResourceResponseMapper responseMapper;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public List<ResourceResponse> getContent(Long userId, String path) {
        List<ResourceMetadataDto> content = metadataService.findDirectoryContent(userId, path);
        return responseMapper.fromResourceMetadataDto(content);
    }

    @Override
    @Transactional
    public ResourceResponse createDirectory(Long userId, String path) {
        validate(userId, path);
        ResourceMetadataDto savedDirectory = metadataService.saveDirectory(userId, path);

        log.info("Created directory: {}", path);
        return responseMapper.toCreatedDirectoryResponse(savedDirectory);
    }

    private void validate(Long userId, String path) {
        String parentPath = PathUtils.getParentPath(path);

        if (!parentPath.isEmpty() && !metadataService.existsByPath(userId, parentPath)) {
            throw new ResourceNotFoundException("Directory not found", parentPath);
        }

        metadataService.throwIfAnyConflictingTypeExists(userId, List.of(path));
    }
}
