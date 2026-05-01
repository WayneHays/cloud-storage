package com.waynehays.cloudfilestorage.files.operation.download;

import com.waynehays.cloudfilestorage.core.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.files.dto.internal.DownloadResult;
import com.waynehays.cloudfilestorage.infrastructure.path.PathUtils;
import com.waynehays.cloudfilestorage.infrastructure.storage.InputStreamSupplier;
import com.waynehays.cloudfilestorage.infrastructure.storage.ResourceStorageServiceApi;
import com.waynehays.cloudfilestorage.infrastructure.storage.StorageItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
class ResourceDownloadService implements ResourceDownloadServiceApi {
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final ArchiverApi archiver;
    private final ResourceStorageServiceApi storageService;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public DownloadResult download(Long userId, String path) {
        ResourceMetadataDto metadata = metadataService.findOrThrow(userId, path);
        String resourceName = PathUtils.extractDisplayName(path);

        if (metadata.isFile()) {
            return downloadFile(userId, metadata, resourceName);
        } else {
            return downloadDirectory(userId, path, resourceName);
        }
    }

    private DownloadResult downloadFile(Long userId, ResourceMetadataDto metadata, String filename) {
        log.info("Start prepare file for download: {}", metadata.path());

        InputStreamSupplier contentSupplier = () -> {
            Optional<StorageItem> item = storageService.getObject(userId, metadata.storageKey());

            if (item.isEmpty()) {
                log.error("Data inconsistency: file exists in metadata but not in storage: path={}, storageKey={}",
                        metadata.path(), metadata.storageKey());
                throw new ResourceNotFoundException("Resource not found in storage", metadata.path());
            }
            return item.get().inputStream();
        };

        return new DownloadResult.File(contentSupplier, filename, DEFAULT_CONTENT_TYPE);
    }

    private DownloadResult downloadDirectory(Long userId, String path, String directoryName) {
        log.info("Start prepare directory for download: {}", path);

        List<ArchiveItem> archiveItems = metadataService.findFilesByPathPrefix(userId, path)
                .stream()
                .map(metadata -> createArchiveItem(userId, metadata, path))
                .toList();

        DownloadResult.StreamWriter writer = outputStream -> {
            archiver.archiveResources(archiveItems, outputStream);
            log.info("Finished directory download: {}", path);
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
                () -> storageService.getObject(userId, dto.storageKey())
                        .orElseThrow(() -> new ResourceNotFoundException("Resource not found in storage", dto.path()))
                        .inputStream());
    }
}
