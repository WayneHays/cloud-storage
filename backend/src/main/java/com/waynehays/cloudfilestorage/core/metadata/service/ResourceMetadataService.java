package com.waynehays.cloudfilestorage.core.metadata.service;

import com.waynehays.cloudfilestorage.core.metadata.dto.CreateFileDto;
import com.waynehays.cloudfilestorage.core.metadata.dto.DeleteDirectoryResult;
import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.core.metadata.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.core.metadata.factory.ResourceMetadataFactory;
import com.waynehays.cloudfilestorage.core.metadata.mapper.ResourceMetadataMapper;
import com.waynehays.cloudfilestorage.core.metadata.repository.ResourceMetadataRepository;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
class ResourceMetadataService implements ResourceMetadataServiceApi {
    private final ResourceMetadataMapper mapper;
    private final ResourceMetadataFactory factory;
    private final ResourceMetadataRepository repository;

    @Override
    public boolean existsByPath(Long userId, String path) {
        String normalizedPath = normalize(path);
        return repository.existsByNormalizedPath(userId, normalizedPath);
    }

    @Override
    public ResourceMetadataDto findByPath(Long userId, String path) {
        String normalizedPath = normalize(path);
        ResourceMetadata metadata = repository.findByNormalizedPath(userId, normalizedPath)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found", path));
        return mapper.toDto(metadata);
    }

    @Override
    public List<ResourceMetadataDto> findDirectoryContent(Long userId, String path) {
        String normalizedPath = normalize(path);
        List<ResourceMetadata> result = repository.findByParentPath(userId, normalizedPath);

        if (result.isEmpty() && StringUtils.isNotEmpty(path) && !repository.existsByNormalizedPath(userId, normalizedPath)) {
            throw new ResourceNotFoundException("Resource not found", path);
        }

        return mapper.toDto(result);
    }

    @Override
    public List<ResourceMetadataDto> findFilesByPathPrefix(Long userId, String prefix) {
        String normalizedPrefix = normalize(prefix);
        List<ResourceMetadata> files = repository.findFilesByPathPrefix(userId, normalizedPrefix);
        return mapper.toDto(files);
    }

    @Override
    public List<ResourceMetadataDto> findByNameContaining(Long userId, String query, int limit) {
        List<ResourceMetadata> result = repository.findByNameContaining(userId, query,
                Pageable.ofSize(limit));
        return mapper.toDto(result);
    }

    @Override
    public List<ResourceMetadataDto> findFilesMarkedForDeletion(int limit) {
        List<ResourceMetadata> files = repository.findFilesMarkedForDeletion(Pageable.ofSize(limit));
        return mapper.toDto(files);
    }

    @Override
    @Transactional
    public DeleteDirectoryResult markDirectoryForDeletionAndCollectKeys(Long userId, String path) {
        String normalizedPath = normalize(path);
        List<ResourceMetadata> files = repository.findFilesByPathPrefix(userId, normalizedPath);
        repository.markForDeletionByNormalizedPath(userId, normalizedPath);

        long totalSize = files.stream()
                .mapToLong(ResourceMetadata::getSize)
                .sum();
        List<String> storageKeys = files.stream()
                .map(ResourceMetadata::getStorageKey)
                .toList();

        return new DeleteDirectoryResult(totalSize, storageKeys);
    }

    @Override
    public void throwIfAnyExists(Long userId, List<String> paths) {
        Set<String> normalizedPaths = paths.stream()
                .map(this::normalize)
                .collect(Collectors.toSet());
        Set<String> existing = repository.findExistingPaths(userId, normalizedPaths);

        if (!existing.isEmpty()) {
            throw new ResourceAlreadyExistsException("Resources already exist", new ArrayList<>(existing));
        }
    }

    @Override
    public void throwIfAnyConflictingTypeExists(Long userId, List<String> paths) {
        Set<String> conflictPaths = paths.stream()
                .map(PathUtils::toOppositeTypePath)
                .map(this::normalize)
                .collect(Collectors.toSet());

        Set<String> conflicts = repository.findExistingPaths(userId, conflictPaths);

        if (!conflicts.isEmpty()) {
            throw new ResourceAlreadyExistsException("Resources with same name, but different type already exist",
                    new ArrayList<>(conflicts));
        }
    }

    @Override
    @Transactional
    public void saveFiles(Long userId, List<CreateFileDto> createFileDtos) {
        log.debug("Start save files metadata: count={}", createFileDtos.size());
        List<ResourceMetadata> entities = factory.createFiles(userId, createFileDtos);
        repository.saveAllAndFlush(entities);

        log.debug("Finished save files metadata: count={}", createFileDtos.size());
    }

    @Override
    @Transactional
    public List<ResourceMetadataDto> saveDirectories(Long userId, Set<String> paths) {
        int directoriesCount = paths.size();
        log.debug("Start save created directories metadata: count={}", directoriesCount);

        Set<String> existingPaths = repository.findExistingNormalizedPaths(userId, paths);
        Set<String> missingPaths = paths.stream()
                .filter(path -> !existingPaths.contains(path))
                .collect(Collectors.toSet());

        if (missingPaths.isEmpty()) {
            return List.of();
        }

        List<ResourceMetadata> directories = factory.createDirectories(userId, missingPaths);
        repository.saveAllAndFlush(directories);
        log.debug("Finished save created directories: count={}", directoriesCount);

        return mapper.toDto(directories);
    }

    @Override
    @Transactional
    public ResourceMetadataDto saveDirectory(Long userId, String path) {
        log.debug("Start create directory: {}", path);
        try {
            ResourceMetadata directory = factory.createDirectory(userId, path);
            repository.saveAndFlush(directory);
            log.debug("Finished create directory: {}", path);
            return mapper.toDto(directory);
        } catch (DataIntegrityViolationException e) {
            throw new ResourceAlreadyExistsException("Directory already exists", path);
        }
    }

    @Override
    @Transactional
    public void moveMetadata(Long userId, String pathFrom, String pathTo) {
        log.debug("Start move metadata: from={}, to={}", pathFrom, pathTo);

        String normalizedPathFrom = normalize(pathFrom);
        String targetParentPath = normalize(PathUtils.getParentPath(pathTo));
        String targetName = PathUtils.getName(pathTo);

        int countMoved = repository.moveMetadata(
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
    public void deleteFileMetadata(Long userId, String path) {
        log.debug("Start delete file metadata: {}", path);
        String normalizedPath = normalize(path);
        repository.deleteFileByNormalizedPath(userId, normalizedPath);
        log.debug("Finished delete file metadata: {}", path);
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

    private String normalize(String path) {
        return PathUtils.normalizePath(path);
    }
}
