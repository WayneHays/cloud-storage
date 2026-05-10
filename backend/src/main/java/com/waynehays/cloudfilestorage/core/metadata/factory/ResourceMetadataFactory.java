package com.waynehays.cloudfilestorage.core.metadata.factory;

import com.waynehays.cloudfilestorage.core.metadata.dto.CreateFileDto;
import com.waynehays.cloudfilestorage.core.metadata.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class ResourceMetadataFactory {

    public ResourceMetadata createDirectory(Long userId, String path) {
        String directoryPath = PathUtils.ensureTrailingSlash(path);
        return new ResourceMetadata(
                userId,
                directoryPath,
                PathUtils.normalizePath(directoryPath),
                PathUtils.getParentPath(path),
                PathUtils.getName(path)
        );
    }

    public ResourceMetadata createFile(Long userId, String storageKey, String path, Long size) {
        String filePath = PathUtils.removeTrailingSlash(path);
        return new ResourceMetadata(
                userId,
                storageKey,
                filePath,
                PathUtils.normalizePath(filePath),
                PathUtils.getParentPath(path),
                PathUtils.getName(path),
                size
        );
    }

    public List<ResourceMetadata> createFiles(Long userId, List<CreateFileDto> fileData) {
        return fileData.stream()
                .map(fd -> createFile(userId, fd.storageKey(), fd.path(), fd.size()))
                .toList();
    }

    public List<ResourceMetadata> createDirectories(Long userId, Set<String> paths) {
        return paths.stream()
                .map(p -> createDirectory(userId, p))
                .toList();
    }
}
