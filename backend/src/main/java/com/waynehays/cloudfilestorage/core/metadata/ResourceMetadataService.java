package com.waynehays.cloudfilestorage.core.metadata;

import com.waynehays.cloudfilestorage.core.metadata.dto.DirectoryRowDto;
import com.waynehays.cloudfilestorage.core.metadata.dto.FileRowDto;
import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.infrastructure.path.PathUtils;
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
class ResourceMetadataService implements ResourceMetadataServiceApi {
    private final ResourceMetadataMapper mapper;
    private final ResourceMetadataRepository repository;

    @Override
    public boolean existsByPath(Long userId, String path) {
        String normalizedPath = normalize(path);
        return repository.existsByNormalizedPath(userId, normalizedPath);
    }

    @Override
    public ResourceMetadataDto findOrThrow(Long userId, String path) {
        String normalizedPath = normalize(path);
        ResourceMetadata metadata = repository.findByNormalizedPath(userId, normalizedPath)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found", path));
        return mapper.toResourceMetadataDto(metadata);
    }

    @Override
    public List<ResourceMetadataDto> findDirectoryContent(Long userId, String path) {
        String normalizedPath = normalize(path);
        List<ResourceMetadata> result = repository.findByParentPath(userId, normalizedPath);

        if (result.isEmpty() && StringUtils.isNotEmpty(path)) {
            if (!repository.existsByNormalizedPath(userId, normalizedPath)) {
                throw new ResourceNotFoundException("Resource not found", path);
            }
        }

        return mapper.toResourceMetadataDto(result);
    }

    @Override
    public List<ResourceMetadataDto> findFilesByPathPrefix(Long userId, String prefix) {
        String normalizedPrefix = normalize(prefix);
        List<ResourceMetadata> files = repository.findFilesByPathPrefix(userId, normalizedPrefix);
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
        if (paths.isEmpty()) {
            return Set.of();
        }

        Set<String> normalizedPaths = normalizePaths(paths);
        return repository.findExistingPaths(userId, normalizedPaths);
    }

    @Override
    public Set<String> findMissingPaths(Long userId, Set<String> paths) {
        if (paths.isEmpty()) {
            return Set.of();
        }
        Set<String> normalizedPaths = normalizePaths(paths);
        return repository.findMissingPaths(userId, normalizedPaths);
    }

    @Override
    @Transactional
    public long markDirectoryForDeletionAndSumSize(Long userId, String path) {
        return repository.markForDeletionAndSumSize(userId, normalize(path));
    }

    @Override
    @Transactional
    public void saveFiles(Long userId, List<FileRowDto> files) {
        int filesCount = files.size();
        log.debug("Start save files metadata: count={}", filesCount);
        repository.batchSaveFiles(userId, files);
        log.debug("Finished save files metadata: count={}", filesCount);
    }

    @Override
    @Transactional
    public void saveDirectories(Long userId, List<DirectoryRowDto> directories) {
        int directoriesCount = directories.size();
        log.debug("Start save created directories metadata: count={}", directoriesCount);
        repository.batchSaveDirectories(userId, directories);
        log.debug("Finished save created directories metadata: count={}", directoriesCount);
    }

    @Override
    @Transactional
    public void saveDirectory(Long userId, String path) {
        log.debug("Start create directory: {}", path);
        try {
            ResourceMetadata directory = mapper.toDirectoryEntity(userId, path);
            repository.saveAndFlush(directory);
        } catch (DataIntegrityViolationException e) {
            throw new ResourceAlreadyExistsException("Directory already exists", path);
        }
        log.debug("Finished create directory: {}", path);
    }

    @Override
    @Transactional
    public void moveMetadata(Long userId, String pathFrom, String pathTo) {
        log.debug("Start move metadata: from={}, to={}", pathFrom, pathTo);

        String normalizedPathFrom = normalize(pathFrom);
        String targetParentPath = normalize(PathUtils.extractParentPath(pathTo));
        String targetName = PathUtils.extractDisplayName(pathTo);

        int countMoved  = repository.moveMetadata(
                userId, normalizedPathFrom, pathTo, targetParentPath, targetName);

        if (countMoved == 0) {
            throw new ResourceNotFoundException("Resource not found", pathFrom);
        }

        log.debug("Finished move metadata: {} resources moved", countMoved);
    }

    @Override
    @Transactional
    public void markForDeletion(Long userId, String path) {
        String normalizedPath = normalize(path);
        repository.markForDeletionByNormalizedPath(userId, normalizedPath);
    }

    @Override
    @Transactional
    public void deleteFileByPath(Long userId, String path) {
        log.debug("Start delete metadata: {}", path);
        String normalizedPath = normalize(path);
        repository.deleteFileByNormalizedPath(userId, normalizedPath);
        log.debug("Finished delete metadata: {}", path);
    }

    @Override
    @Transactional
    public void deleteDirectoryMetadata(Long userId, String directoryPath) {
        log.debug("Start delete directory metadata: {}", directoryPath);
        String normalizedPath = normalize(directoryPath);
        repository.deleteByNormalizedPathPrefix(userId, normalizedPath);
        log.debug("Finished delete directory metadata: {}", directoryPath);
    }

    @Override
    @Transactional
    public void deleteByPaths(Long userId, List<String> paths) {
        log.debug("Start delete metadata by paths: {}", paths);
        List<String> normalizedPaths = paths.stream()
                .map(this::normalize)
                .toList();
        repository.deleteByNormalizedPaths(userId, normalizedPaths);
        log.debug("Finished delete metadata by paths: {}", paths);
    }

    @Override
    @Transactional
    public void deleteByIds(List<Long> ids) {
        log.debug("Start delete metadata by ids={}", ids);
        repository.deleteByIds(ids);
        log.debug("Finished delete metadata by ids={}", ids);
    }

    private Set<String> normalizePaths(Set<String> paths) {
        return paths.stream()
                .map(this::normalize)
                .collect(Collectors.toSet());
    }

    private String normalize(String path) {
        return PathUtils.normalizePath(path);
    }
}
