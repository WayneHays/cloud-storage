package com.waynehays.cloudfilestorage.service.resource.downloader;

import com.waynehays.cloudfilestorage.component.archiver.ArchiveItem;
import com.waynehays.cloudfilestorage.component.archiver.ArchiverApi;
import com.waynehays.cloudfilestorage.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.dto.response.DownloadResult;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageKeyResolverApi;
import com.waynehays.cloudfilestorage.storage.dto.StorageItem;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceDownloader implements ResourceDownloaderApi {
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final ResourceStorageApi resourceStorage;
    private final ResourceStorageKeyResolverApi keyResolver;
    private final ResourceMetadataServiceApi metadataService;
    private final ArchiverApi archiver;

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
        log.info("Start download file: userId={}, path={}", userId, path);

        String objectKey = keyResolver.resolveKey(userId, path);
        StorageItem item = resourceStorage.getObject(objectKey).orElseThrow();

        StreamingResponseBody body = outputStream -> {
            try (InputStream inputStream = item.inputStream()) {
                inputStream.transferTo(outputStream);
            }
        };

        log.info("Successfully downloaded file: userId={}, path={}", userId, path);
        return new DownloadResult(body, filename, DEFAULT_CONTENT_TYPE);
    }

    private DownloadResult downloadDirectory(Long userId, String path, String directoryName) {
        log.info("Start download directory: userId={}, path={}", userId, path);

        List<ArchiveItem> archiveItems = metadataService.findDirectoryContent(userId, path)
                .stream()
                .filter(ResourceMetadataDto::isFile)
                .map(metadata -> createArchiveItem(userId, metadata, path))
                .toList();

        StreamingResponseBody body = outputStream -> archiver.archiveResources(archiveItems, outputStream);

        log.info("Successfully downloaded directory: userId={}, path={}", userId, path);
        return new DownloadResult(body, directoryName + archiver.getExtension(), archiver.getContentType());
    }

    private ArchiveItem createArchiveItem(Long userId, ResourceMetadataDto dto, String directoryPath) {
        String storageKey = keyResolver.resolveKey(userId, dto.path());
        String entryName = dto.path().substring(directoryPath.length());

        return new ArchiveItem(entryName, dto.size(),
                () -> resourceStorage.getObject(storageKey)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Resource not found in storage", dto.path()))
                        .inputStream());
    }
}
