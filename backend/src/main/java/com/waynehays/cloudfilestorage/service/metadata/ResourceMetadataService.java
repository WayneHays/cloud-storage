package com.waynehays.cloudfilestorage.service.metadata;

import com.waynehays.cloudfilestorage.dto.internal.NewFileDto;
import com.waynehays.cloudfilestorage.dto.internal.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.mapper.ResourceMetadataMapper;
import com.waynehays.cloudfilestorage.repository.ResourceMetadataRepository;
import com.waynehays.cloudfilestorage.service.storagequota.UsedSpace;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ResourceMetadataService implements ResourceMetadataServiceApi {
    private static final String SLASH = "/";

    private final ResourceMetadataMapper mapper;
    private final ResourceMetadataRepository repository;

    @Override
    public ResourceMetadataDto findOrThrow(Long userId, String path) {
        ResourceMetadata metadata = repository.findByPath(userId, path)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found", path));
        return mapper.toDto(metadata);
    }

    @Override
    public List<ResourceMetadataDto> findDirectChildren(Long userId, String directoryPath) {
        if (StringUtils.isNotEmpty(directoryPath)) {
            findOrThrow(userId, directoryPath);
        }
        List<ResourceMetadata> result = repository.findDirectChildren(userId, directoryPath);
        return mapper.toDto(result);
    }

    @Override
    public List<ResourceMetadataDto> findFilesByPrefix(Long userId, String prefix) {
        List<ResourceMetadata> files = repository.findFilesByPrefix(userId, prefix);
        return mapper.toDto(files);
    }

    @Override
    public List<ResourceMetadataDto> findAllByPrefix(Long userId, String pathPrefix) {
        List<ResourceMetadata> result = repository.findAllByPrefix(userId, pathPrefix);
        return mapper.toDto(result);
    }

    @Override
    public List<ResourceMetadataDto> findByNameContaining(Long userId, String query) {
        List<ResourceMetadata> result = repository.findByNameContaining(userId, query);
        return mapper.toDto(result);
    }

    @Override
    public List<ResourceMetadataDto> findMarkedForDeletion() {
        List<ResourceMetadata> result = repository.findByMarkedForDeletionTrue();
        return mapper.toDto(result);
    }

    @Override
    public Set<String> findExistingPaths(Long userId, Set<String> paths) {
        return repository.findExistingPaths(userId, paths);
    }

    @Override
    public List<UsedSpace> getUsedSpaceOfUsers(List<Long> userIds) {
        return repository.sumSizeGroupByUserId(userIds, ResourceType.FILE);
    }

    @Override
    public long sumResourceSizesByPrefix(Long userId, String prefix) {
        return repository.sumSizeByPrefix(userId, prefix, ResourceType.FILE);
    }

    @Override
    public void validateDirectoryCreation(Long userId, String path) {
        String parentPath = PathUtils.extractParentPath(path);
        List<String> paths = List.of(path, parentPath);
        Set<String> existing = repository.findExistingPaths(userId, new HashSet<>(paths));

        if (existing.contains(path)) {
            throw new ResourceAlreadyExistsException("Directory already exists", path);
        }

        if (!parentPath.isEmpty() && !existing.contains(parentPath)) {
            throw new ResourceNotFoundException("Directory not found", parentPath);
        }
    }

    @Override
    public void throwIfAnyExists(Long userId, List<String> paths) {
        Set<String> existing = repository.findExistingPaths(userId, new HashSet<>(paths));
        if (!existing.isEmpty()) {
            throw new ResourceAlreadyExistsException("Resources already exist", new ArrayList<>(existing));
        }
    }

    @Override
    @Transactional
    public void saveFile(Long userId, String path, long size) {
        if (path.endsWith(SLASH)) {
            throw new IllegalArgumentException("File path must not end with %s : %s".formatted(SLASH, path));
        }
        ResourceMetadata metadata = mapper.toFile(userId, path, size);
        repository.save(metadata);
    }

    @Override
    @Transactional
    public void saveFiles(Long userId, List<NewFileDto> files) {
        List<ResourceMetadata> entities = files.stream()
                .map(f -> mapper.toFile(userId, f.path(), f.size()))
                .toList();
        repository.saveAll(entities);
    }

    @Override
    @Transactional
    public void saveDirectories(Long userId, Set<String> paths) {
        List<Object[]> params = paths.stream()
                .map(path -> new Object[]{
                        userId,
                        path,
                        PathUtils.extractParentPath(path),
                        PathUtils.extractFilename(path)
                })
                .toList();

        repository.insertDirectoriesIfNotExist(params);
    }

    @Override
    @Transactional
    public void updatePathsByPrefix(Long userId, String prefixFrom, String prefixTo) {
        repository.updatePathsByPrefix(userId, prefixFrom, prefixTo);
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
    public void delete(Long userId, String path) {
        repository.deleteByPath(userId, path);
    }

    @Override
    @Transactional
    public void deleteByPrefix(Long userId, String pathPrefix) {
        repository.deleteByPrefix(userId, pathPrefix);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    @Transactional
    public int deleteStaleDeletionRecords(Instant threshold) {
        return repository.deleteStaleDeletionRecords(threshold);
    }
}
