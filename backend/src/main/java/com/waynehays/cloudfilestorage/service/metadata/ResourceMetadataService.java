package com.waynehays.cloudfilestorage.service.metadata;

import com.waynehays.cloudfilestorage.dto.internal.metadata.DirectoryRowDto;
import com.waynehays.cloudfilestorage.dto.internal.metadata.FileRowDto;
import com.waynehays.cloudfilestorage.dto.internal.metadata.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.mapper.ResourceMetadataMapper;
import com.waynehays.cloudfilestorage.repository.metadata.ResourceMetadataRepository;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ResourceMetadataService implements ResourceMetadataServiceApi {
    private final ResourceMetadataMapper mapper;
    private final ResourceMetadataRepository repository;

    @Override
    public boolean existsByPath(Long userId, String path) {
        return repository.existsByNormalizedPath(userId, normalize(path));
    }

    @Override
    public ResourceMetadataDto findOrThrow(Long userId, String path) {
        ResourceMetadata metadata = repository.findByNormalizedPath(userId, normalize(path))
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found", path));
        return mapper.toResourceMetadataDto(metadata);
    }

    @Override
    public List<ResourceMetadataDto> findDirectoryContent(Long userId, String path) {
        List<ResourceMetadata> result = repository.findByParentPath(userId, normalize(path));

        if (result.isEmpty() && StringUtils.isNotEmpty(path)) {
            if (!repository.existsByNormalizedPath(userId, normalize(path))) {
                throw new ResourceNotFoundException("Resource not found", path);
            }
        }

        return mapper.toResourceMetadataDto(result);
    }

    @Override
    public List<ResourceMetadataDto> findFilesByPathPrefix(Long userId, String prefix) {
        List<ResourceMetadata> files = repository.findFilesByPathPrefix(userId, normalize(prefix));
        return mapper.toResourceMetadataDto(files);
    }

    @Override
    public List<ResourceMetadataDto> findByNameContaining(Long userId, String query, int limit) {
        List<ResourceMetadata> result = repository.findByNameContaining(userId, query,
                Pageable.ofSize(limit));
        return mapper.toResourceMetadataDto(result);
    }

    @Override
    public List<ResourceMetadataDto> findFilesMarkedForDeletion(int limit) {
        List<ResourceMetadata> files = repository.findFilesMarkedForDeletion(Pageable.ofSize(limit));
        return mapper.toResourceMetadataDto(files);
    }

    @Override
    public Set<String> findExistingPaths(Long userId, Set<String> paths) {
        Set<String> normalizedPaths = paths.stream()
                .map(PathUtils::normalizePath)
                .collect(Collectors.toSet());
        return repository.findExistingPaths(userId, normalizedPaths);
    }

    @Override
    public Set<String> findMissingPaths(Long userId, Set<String> paths) {
        return repository.findMissingPaths(userId, paths);
    }

    @Override
    @Transactional
    public long markForDeletionAndSumFileSize(Long userId, String path) {
        return repository.markForDeletionAndSumSize(userId, normalize(path));
    }

    @Override
    @Transactional
    public void saveFiles(Long userId, List<FileRowDto> files) {
        int filesCount = files.size();
        log.debug("Start save files metadata for userId={}, files={}", userId, filesCount);
        repository.batchSaveFiles(userId, files);
        log.debug("Finished save files metadata for userId={}, files={}", userId, filesCount);
    }

    @Override
    @Transactional
    public void saveDirectories(Long userId, List<DirectoryRowDto> directories) {
        int directoriesCount = directories.size();
        log.debug("Start save created directories metadata for userId={}, directories={}", userId, directoriesCount);
        repository.batchSaveDirectories(userId, directories);
        log.debug("Finished save created directories metadata for userId={}, directories={}", userId, directoriesCount);
    }

    @Override
    @Transactional
    public void saveDirectory(Long userId, String path) {
        log.debug("Start create directory: userId={}, path={}", userId, path);
        try {
            ResourceMetadata directory = mapper.toDirectoryEntity(userId, path);
            repository.saveAndFlush(directory);
        } catch (DataIntegrityViolationException e) {
            throw new ResourceAlreadyExistsException("Directory already exists", path);
        }
        log.debug("Finished create directory: userId={}, path={}", userId, path);
    }

    @Override
    @Transactional
    public void moveMetadata(Long userId, String pathFrom, String pathTo) {
        log.debug("Start move metadata: userId={}, from={}, to={}", userId, pathFrom, pathTo);

        String normalizedPathFrom = normalize(pathFrom);
        String targetParentPath = normalize(PathUtils.extractParentPath(pathTo));
        String targetName = PathUtils.extractDisplayName(pathTo);

        int updated = repository.moveMetadata(
                userId, normalizedPathFrom, pathTo, targetParentPath, targetName);

        if (updated == 0) {
            throw new ResourceNotFoundException("Resource not found", pathFrom);
        }

        log.debug("Finished move metadata: {} resources updated", updated);
    }

    @Override
    @Transactional
    public void markForDeletion(Long userId, String path) {
        repository.markForDeletionByNormalizedPath(userId, normalize(path));
    }

    @Override
    @Transactional
    public void deleteFileByPath(Long userId, String path) {
        log.debug("Start delete metadata: userId={}, path={}", userId, path);
        repository.deleteFileByNormalizedPath(userId, normalize(path));
        log.debug("Finished delete metadata: userId={}, path={}", userId, path);
    }

    @Override
    @Transactional
    public void deleteDirectoryMetadata(Long userId, String pathPrefix) {
        log.debug("Start delete directory metadata: userId={}, prefix={}", userId, pathPrefix);
        repository.deleteByNormalizedPathPrefix(userId, normalize(pathPrefix));
        log.debug("Finished delete directory metadata: userId={}, prefix={}", userId, pathPrefix);
    }

    @Override
    @Transactional
    public void deleteByPaths(Long userId, List<String> paths) {
        log.debug("Start delete metadata by paths: userId={}, paths={}", userId, paths);
        List<String> normalizedPaths = paths.stream()
                .map(PathUtils::normalizePath)
                .toList();
        repository.deleteByNormalizedPaths(userId, normalizedPaths);
        log.debug("Finished delete metadata by paths: userId={}, paths={}", userId, paths);
    }

    @Override
    @Transactional
    public void deleteByIds(List<Long> ids) {
        log.debug("Start delete metadata by ids={}", ids);
        repository.deleteByIds(ids);
        log.debug("Finished delete metadata by ids={}", ids);
    }

    private String normalize(String path) {
        return PathUtils.normalizePath(path);
    }
}
