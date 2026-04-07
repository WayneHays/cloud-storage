package com.waynehays.cloudfilestorage.service.metadata;

import com.waynehays.cloudfilestorage.dto.internal.NewFileDto;
import com.waynehays.cloudfilestorage.dto.internal.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.entity.ResourceType;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.mapper.ResourceMetadataMapper;
import com.waynehays.cloudfilestorage.repository.ResourceMetadataRepository;
import com.waynehays.cloudfilestorage.service.quota.UsedSpace;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ResourceMetadataService implements ResourceMetadataServiceApi {
    private final ResourceMetadataMapper mapper;
    private final ResourceMetadataRepository repository;

    @Override
    public ResourceMetadataDto findOrThrow(Long userId, String path) {
        ResourceMetadata metadata = repository.findByPath(userId, path)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found", path));
        return mapper.toResourceMetadataDto(metadata);
    }

    @Override
    public List<ResourceMetadataDto> findDirectoryContent(Long userId, String path) {
        if (StringUtils.isNotEmpty(path)) {
            findOrThrow(userId, path);
        }
        List<ResourceMetadata> result = repository.findByParentPath(userId, path);
        return mapper.toResourceMetadataDto(result);
    }

    @Override
    public List<ResourceMetadataDto> findFilesByPathPrefix(Long userId, String prefix) {
        List<ResourceMetadata> files = repository.findFilesByPathPrefix(userId, prefix);
        return mapper.toResourceMetadataDto(files);
    }

    @Override
    public List<ResourceMetadataDto> findAllByPathPrefix(Long userId, String prefix) {
        List<ResourceMetadata> result = repository.findAllByPathPrefix(userId, prefix);
        return mapper.toResourceMetadataDto(result);
    }

    @Override
    public List<ResourceMetadataDto> findByNameContaining(Long userId, String query, int limit) {
        List<ResourceMetadata> result = repository.findByNameContaining(userId, query,
                Pageable.ofSize(limit));
        return mapper.toResourceMetadataDto(result);
    }

    @Override
    public List<ResourceMetadataDto> findMarkedForDeletion(int limit) {
        List<ResourceMetadata> result = repository.findByMarkedForDeletionTrue(Pageable.ofSize(limit));
        return mapper.toResourceMetadataDto(result);
    }

    @Override
    public Set<String> findExistingPaths(Long userId, Set<String> paths) {
        return repository.findExistingPaths(userId, paths);
    }

    @Override
    public List<UsedSpace> getUsedSpaceByUsers(List<Long> userIds) {
        return repository.sumFileSizesGroupByUserId(userIds, ResourceType.FILE);
    }

    @Override
    public long sumFileSizesByPathPrefix(Long userId, String prefix) {
        return repository.sumFileSizesByPathPrefix(userId, prefix, ResourceType.FILE);
    }


    @Override
    @Transactional
    public void saveFiles(Long userId, List<NewFileDto> files) {
        List<ResourceMetadata> entities = mapper.toFileEntity(userId, files);
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

        repository.saveDirectoriesIfNotExist(params);
    }

    @Override
    @Transactional
    public void saveDirectory(Long userId, String path) {
        ResourceMetadata directory = mapper.toDirectoryEntity(userId, path);
        repository.save(directory);
    }

    @Override
    @Transactional
    public void updatePathsByPrefix(Long userId, String prefixFrom, String prefixTo) {
        repository.updatePathsByPathPrefix(userId, prefixFrom, prefixTo);
    }

    @Override
    @Transactional
    public void markForDeletion(Long userId, String path) {
        repository.markForDeletionByPath(userId, path);
    }

    @Override
    @Transactional
    public void markForDeletionByPathPrefix(Long userId, String pathPrefix) {
        repository.markForDeletionByPathPrefix(userId, pathPrefix);
    }

    @Override
    @Transactional
    public void deleteByPath(Long userId, String path) {
        repository.deleteByPath(userId, path);
    }

    @Override
    @Transactional
    public void deleteByPathPrefix(Long userId, String pathPrefix) {
        repository.deleteByPathPrefix(userId, pathPrefix);
    }

    @Override
    @Transactional
    public void deleteByPaths(Long userId, List<String> paths) {
        repository.deleteByPaths(userId, paths);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    @Transactional
    public int deleteStaleMarkedRecords(Instant threshold) {
        return repository.deleteStaleMarkedRecords(threshold);
    }
}
