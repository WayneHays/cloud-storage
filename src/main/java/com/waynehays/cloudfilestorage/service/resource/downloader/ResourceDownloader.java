package com.waynehays.cloudfilestorage.service.resource.downloader;

import com.waynehays.cloudfilestorage.constant.Messages;
import com.waynehays.cloudfilestorage.component.archiver.ArchiverApi;
import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import com.waynehays.cloudfilestorage.component.archiver.ArchiveItem;
import com.waynehays.cloudfilestorage.dto.response.DownloadResult;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.storage.dto.StorageItem;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceDownloader implements ResourceDownloaderApi {
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final ArchiverApi archiver;
    private final ResourceStorageApi fileStorage;
    private final StorageKeyResolverApi keyResolver;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public DownloadResult download(Long userId, String path) {
        metadataService.findOrThrow(userId, path);
        String resourceName = PathUtils.extractFilename(path);

        if (PathUtils.isFile(path)) {
            return downloadFile(userId, path, resourceName);
        } else {
            return downloadDirectory(userId, path, resourceName);
        }
    }

    private DownloadResult downloadFile(Long userId, String path, String filename) {
        String objectKey = keyResolver.resolveKey(userId, path);
        StorageItem item = fileStorage.getObject(objectKey).orElseThrow();

        StreamingResponseBody body = outputStream -> {
            try (InputStream inputStream = item.inputStream()) {
                inputStream.transferTo(outputStream);
            }
        };

        return new DownloadResult(body, filename, DEFAULT_CONTENT_TYPE);
    }

    private DownloadResult downloadDirectory(Long userId, String path, String directoryName) {
        List<ArchiveItem> archiveItems = metadataService.findDirectoryContent(userId, path)
                .stream()
                .filter(ResourceMetadata::isFile)
                .map(metadata -> createArchiveItem(userId, metadata, path))
                .toList();

        StreamingResponseBody body = outputStream -> archiver.archiveResources(archiveItems, outputStream);
        return new DownloadResult(body, directoryName + archiver.getExtension(), archiver.getContentType());
    }

    private ArchiveItem createArchiveItem(Long userId, ResourceMetadata resourceMetadata, String directoryPath) {
        String storageKey = keyResolver.resolveKey(userId, resourceMetadata.getPath());
        String entryName = resourceMetadata.getPath().substring(directoryPath.length());

        return new ArchiveItem(entryName, resourceMetadata.getSize(),
                () -> fileStorage.getObject(storageKey)
                        .orElseThrow(() -> new ResourceNotFoundException(Messages.NOT_FOUND + resourceMetadata.getPath()))
                        .inputStream());
    }
}
