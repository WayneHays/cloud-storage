package com.waynehays.cloudfilestorage.component.archiver;

import com.waynehays.cloudfilestorage.config.properties.ArchiveProperties;
import com.waynehays.cloudfilestorage.exception.ArchiveException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ZipArchiver implements ArchiverApi {
    private static final String CONTENT_TYPE = "application/zip";
    private static final String EXTENSION = ".zip";

    private final ArchiveProperties archiveProperties;

    @Override
    public void archiveResources(List<ArchiveItem> items, OutputStream outputStream) {
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(outputStream)) {
            for (ArchiveItem item : items) {
                addFileToArchive(zos, item);
            }
            zos.finish();
        } catch (IOException e) {
            throw new ArchiveException("Failed to create ZIP archive", e);
        }
    }

    private void addFileToArchive(ZipArchiveOutputStream zos, ArchiveItem item) throws IOException {
        ZipArchiveEntry entry = new ZipArchiveEntry(item.name());
        entry.setSize(item.size());
        zos.putArchiveEntry(entry);

        try (InputStream inputStream = item.inputStreamSupplier().get();
             BufferedInputStream bis = new BufferedInputStream(inputStream, archiveProperties.bufferSize())) {
            bis.transferTo(zos);
        } finally {
            zos.closeArchiveEntry();
        }
    }

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }

    @Override
    public String getExtension() {
        return EXTENSION;
    }
}
