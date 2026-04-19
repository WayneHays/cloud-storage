package com.waynehays.cloudfilestorage.service.resource.download;

import com.waynehays.cloudfilestorage.archiver.ArchiverApi;
import com.waynehays.cloudfilestorage.dto.internal.ArchiveItem;
import com.waynehays.cloudfilestorage.dto.internal.DownloadResult;
import com.waynehays.cloudfilestorage.dto.internal.metadata.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.storage.InputStreamSupplier;
import com.waynehays.cloudfilestorage.service.storage.ResourceStorageServiceApi;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceDownloadService implements ResourceDownloadServiceApi {
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final ArchiverApi archiver;
    private final ResourceStorageServiceApi storageService;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public DownloadResult download(Long userId, String path) {
        ResourceMetadataDto metadata = metadataService.findOrThrow(userId, path);
        String resourceName = PathUtils.extractDisplayName(path);

        if (metadata.isFile()) {
            return downloadFile(userId, path, resourceName);
        } else {
            return downloadDirectory(userId, path, resourceName);
        }
    }

    private DownloadResult downloadFile(Long userId, String path, String filename) {
        log.info("Start prepare file for download: userId={}, path={}", userId, path);

        InputStreamSupplier contentSupplier = () -> {
            try {
                return storageService.getObject(userId, path).inputStream();
            } catch (ResourceNotFoundException e) {
                log.error("Data inconsistency: file exists in metadata but not in storage. userId={}, path={}", userId, path);
                throw e;
            }
        };

        return new DownloadResult.File(contentSupplier, filename, DEFAULT_CONTENT_TYPE);
    }

    private DownloadResult downloadDirectory(Long userId, String path, String directoryName) {
        log.info("Start prepare directory for download: userId={}, path={}", userId, path);

        List<ArchiveItem> archiveItems = metadataService.findFilesByPathPrefix(userId, path)
                .stream()
                .map(metadata -> createArchiveItem(userId, metadata, path))
                .toList();

        DownloadResult.StreamWriter writer = outputStream -> {
            archiver.archiveResources(archiveItems, outputStream);
            log.info("Finished directory download: userId={}, path={}", userId, path);
        };

        return new DownloadResult.Archive(
                writer,
                directoryName + archiver.getExtension(),
                archiver.getContentType()
        );
    }

    private ArchiveItem createArchiveItem(Long userId, ResourceMetadataDto dto, String directoryPath) {
        String entryName = dto.path().substring(directoryPath.length());
        return new ArchiveItem(
                entryName,
                dto.size(),
                () -> storageService.getObject(userId, dto.path()).inputStream());
    }
}
