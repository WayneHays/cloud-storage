package com.waynehays.cloudfilestorage.service.resource.download;

import com.waynehays.cloudfilestorage.archiver.ArchiverApi;
import com.waynehays.cloudfilestorage.dto.internal.archive.ArchiveItem;
import com.waynehays.cloudfilestorage.dto.internal.metadata.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.dto.response.DownloadResult;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.dto.internal.storage.StorageItem;
import com.waynehays.cloudfilestorage.service.storage.ResourceStorageService;
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
public class ResourceDownloadService implements ResourceDownloadServiceApi {
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final ArchiverApi archiver;
    private final ResourceStorageService storageService;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public DownloadResult download(Long userId, String path) {
        ResourceMetadataDto metadata = metadataService.findOrThrow(userId, path);
        String resourceName = PathUtils.extractFilename(path);

        if (metadata.isFile()) {
            return downloadFile(userId, path, resourceName);
        } else {
            return downloadDirectory(userId, path, resourceName);
        }
    }

    private DownloadResult downloadFile(Long userId, String path, String filename) {
        log.info("Start downloading file: userId={}, path={}", userId, path);
        StorageItem item = storageService.getObject(userId, path);

        StreamingResponseBody body = outputStream -> {
            try (InputStream inputStream = item.inputStream()) {
                inputStream.transferTo(outputStream);
            }
            log.info("Successfully downloaded file: userId={}, path={}", userId, path);
        };

        return new DownloadResult(body, filename, DEFAULT_CONTENT_TYPE);
    }

    private DownloadResult downloadDirectory(Long userId, String path, String directoryName) {
        log.info("Start downloading directory: userId={}, path={}", userId, path);

        List<ArchiveItem> archiveItems = metadataService.findFilesByPathPrefix(userId, path)
                .stream()
                .map(metadata -> createArchiveItem(userId, metadata, path))
                .toList();

        StreamingResponseBody body = outputStream -> {
            archiver.archiveResources(archiveItems, outputStream);
            log.info("Successfully downloaded directory: userId={}, path={}", userId, path);
        };

        return new DownloadResult(body, directoryName + archiver.getExtension(), archiver.getContentType());
    }

    private ArchiveItem createArchiveItem(Long userId, ResourceMetadataDto dto, String directoryPath) {
        String entryName = dto.path().substring(directoryPath.length());
        return new ArchiveItem(
                entryName,
                dto.size(),
                () -> storageService.getObject(userId, dto.path()).inputStream());
    }
}
