package com.waynehays.cloudfilestorage.service.metadata;

import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.repository.ResourceMetadataRepository;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ResourceMetadataService implements ResourceMetadataServiceApi {
    private static final String MSG_NOT_FOUND = "Resource not found: userId=%d, path=%s";
    private static final String MSG_ALREADY_EXISTS = "Resources already exist: userId=%d, paths=%s";

    private final ResourceMetadataRepository repository;

    @Override
    public ResourceMetadata findOrThrow(Long userId, String path) {
        return repository.findByUserIdAndPathAndMarkedForDeletionFalse(userId, path)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_NOT_FOUND.formatted(userId, path)));
    }

    @Override
    public void throwIfExists(Long userId, String path) {
        if (exists(userId, path)) {
            throw new ResourceAlreadyExistsException(MSG_ALREADY_EXISTS.formatted(userId, path));
        }
    }

    @Override
    public void throwIfAnyExists(Long userId, List<String> paths) {
        List<String> existingPaths = paths.stream()
                .filter(path -> exists(userId, path))
                .toList();

        if (ObjectUtils.isNotEmpty(existingPaths)) {
            throw new ResourceAlreadyExistsException(MSG_ALREADY_EXISTS.formatted(userId, existingPaths));
        }
    }

    @Override
    public void ensureParentExists(Long userId, String directoryPath) {
        String parentPath = PathUtils.extractParentPath(directoryPath);

        if (StringUtils.isNotEmpty(parentPath)) {
            findOrThrow(userId, parentPath);
        }
    }

    @Override
    public boolean exists(Long userId, String path) {
        return repository.existsByUserIdAndPathAndMarkedForDeletionFalse(userId, path);
    }

    @Override
    public List<ResourceMetadata> findDirectChildren(Long userId, String directoryPath) {
        if (StringUtils.isNotEmpty(directoryPath)) {
            findOrThrow(userId, directoryPath);
        }
        return repository.findByUserIdAndParentPathAndMarkedForDeletionFalse(userId, directoryPath);
    }

    @Override
    public List<ResourceMetadata> findDirectoryContent(Long userId, String pathPrefix) {
        return repository.findByUserIdAndPathStartingWithAndMarkedForDeletionFalse(userId, pathPrefix);
    }

    @Override
    public List<ResourceMetadata> findByNameContaining(Long userId, String query) {
        return repository.findByUserIdAndNameContainingIgnoreCaseAndMarkedForDeletionFalse(userId, query);
    }

    @Override
    public List<ResourceMetadata> findMarkedForDeletion() {
        return repository.findByMarkedForDeletionTrue();
    }

    @Override
    @Transactional
    public void saveFile(Long userId, String path, long size) {
        save(userId, path, size, ResourceType.FILE);
    }

    @Override
    @Transactional
    public void saveDirectory(Long userId, String path) {
        save(userId, path, null, ResourceType.DIRECTORY);
    }

    @Override
    @Transactional
    public void markForDeletion(Long userId, String path) {
        repository.markForDeletion(userId, path);
    }

    @Override
    @Transactional
    public void markForDeletionByPrefix(Long userId, String pathPrefix) {
        repository.markForDeletionByPrefix(userId, pathPrefix);
    }

    @Override
    @Transactional
    public void updatePath(Long userId, String pathFrom, String pathTo) {
        ResourceMetadata metadata = findOrThrow(userId, pathFrom);
        applyNewPath(metadata, pathFrom, pathTo);
        repository.save(metadata);
    }

    @Override
    @Transactional
    public void batchUpdatePaths(List<ResourceMetadata> resourcesToUpdate, String prefixFrom, String prefixTo) {
        resourcesToUpdate.forEach(resource -> applyNewPath(resource, prefixFrom, prefixTo));
        repository.saveAll(resourcesToUpdate);
    }

    @Override
    @Transactional
    public void delete(Long userId, String path) {
        repository.deleteByUserIdAndPath(userId, path);
    }

    @Override
    @Transactional
    public void deleteByPrefix(Long userId, String pathPrefix) {
        repository.deleteByUserIdAndPathStartingWith(userId, pathPrefix);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void applyNewPath(ResourceMetadata metadata, String prefixFrom, String prefixTo) {
        String newPath = metadata.getPath().replace(prefixFrom, prefixTo);
        metadata.setPath(newPath);
        metadata.setParentPath(PathUtils.extractParentPath(newPath));
        metadata.setName(PathUtils.extractFilename(newPath));
        metadata.setMarkedForDeletion(false);
    }

    private void save(Long userId, String path, Long size, ResourceType type) {
        ResourceMetadata resourceMetadata = new ResourceMetadata();
        resourceMetadata.setUserId(userId);
        resourceMetadata.setPath(path);
        resourceMetadata.setParentPath(PathUtils.extractParentPath(path));
        resourceMetadata.setName(PathUtils.extractFilename(path));
        resourceMetadata.setSize(size);
        resourceMetadata.setType(type);
        resourceMetadata.setMarkedForDeletion(false);
        resourceMetadata.setCreatedAt(Instant.now());
        repository.save(resourceMetadata);
    }
}
