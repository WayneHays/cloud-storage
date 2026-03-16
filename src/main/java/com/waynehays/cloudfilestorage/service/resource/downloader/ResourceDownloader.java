package com.waynehays.cloudfilestorage.service.resource.downloader;

import com.waynehays.cloudfilestorage.constants.Messages;
import com.waynehays.cloudfilestorage.component.archiver.ArchiverApi;
import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import com.waynehays.cloudfilestorage.component.archiver.ArchiveItem;
import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.dto.response.DownloadResult;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.filestorage.FileStorageApi;
import com.waynehays.cloudfilestorage.filestorage.dto.MetaData;
import com.waynehays.cloudfilestorage.filestorage.dto.StorageItem;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceDownloader implements ResourceDownloaderApi {
    private final ArchiverApi archiver;
    private final FileStorageApi fileStorage;
    private final StorageKeyResolverApi keyResolver;

    @Override
    public DownloadResult download(Long userId, String path) {
        String objectKey = keyResolver.resolveKey(userId, path);

        if (!fileStorage.exists(objectKey)) {
            throw new ResourceNotFoundException(Messages.NOT_FOUND + path);
        }

        String resourceName = extractResourceName(path);

        if (PathUtils.isFile(path)) {
            return downloadFile(objectKey, resourceName);
        } else {
            return downloadDirectory(userId, objectKey, resourceName);
        }
    }

    private DownloadResult downloadFile(String objectKey, String filename) {
        StorageItem item = fileStorage.getObject(objectKey).orElseThrow();
        StreamingResponseBody body = outputStream -> {
            try (InputStream inputStream = item.inputStream()) {
                inputStream.transferTo(outputStream);
            }
        };

        return new DownloadResult(body, filename, ResourceType.FILE);
    }

    private DownloadResult downloadDirectory(Long userId, String objectKeyPrefix, String directoryName) {
        List<ArchiveItem> archiveItems = collectArchiveItems(userId, objectKeyPrefix);
        StreamingResponseBody body = outputStream -> archiver.archiveFiles(archiveItems, outputStream);

        return new DownloadResult(body, directoryName + archiver.getExtension(), ResourceType.DIRECTORY);
    }

    private List<ArchiveItem> collectArchiveItems(Long userId, String objectKeyPrefix) {
        return fileStorage.getListRecursive(objectKeyPrefix)
                .stream()
                .filter(metaData -> PathUtils.isFile(metaData.key()))
                .map(metaData -> {
                    String entryName = keyResolver.extractPath(userId, metaData.key());
                    return createArchiveItem(metaData, entryName);
                })
                .toList();
    }

    private ArchiveItem createArchiveItem(MetaData metaData, String entryName) {
        return new ArchiveItem(entryName, metaData.size(),
                () -> fileStorage.getObject(metaData.key())
                        .orElseThrow(() -> new ResourceNotFoundException(Messages.NOT_FOUND + metaData.key()))
                        .inputStream());
    }

    private String extractResourceName(String path) {
        String cleanPath = PathUtils.removeTrailingSeparator(path);
        return PathUtils.extractFilename(cleanPath);
    }
}
