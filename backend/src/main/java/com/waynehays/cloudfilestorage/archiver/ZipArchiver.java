package com.waynehays.cloudfilestorage.archiver;

import com.waynehays.cloudfilestorage.dto.internal.ArchiveItem;
import com.waynehays.cloudfilestorage.exception.ArchiveException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ZipArchiver implements ArchiverApi {
    private static final String CONTENT_TYPE = "application/zip";
    private static final String EXTENSION = ".zip";

    @Override
    public void archiveResources(List<ArchiveItem> items, OutputStream outputStream) {
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(outputStream)) {
            for (ArchiveItem item : items) {
                addFileToArchive(zos, item);
            }
            zos.finish();
        } catch (IOException e) {
            log.error("Failed to create ZIP archive", e);
            throw new ArchiveException("Failed to create ZIP archive", e);
        }
    }

    private void addFileToArchive(ZipArchiveOutputStream zos, ArchiveItem item) throws IOException {
        ZipArchiveEntry entry = new ZipArchiveEntry(item.name());
        entry.setSize(item.size());
        zos.putArchiveEntry(entry);

        try (InputStream inputStream = item.inputStreamSupplier().get()) {
            inputStream.transferTo(zos);
        } catch (IOException e) {
            log.error("Failed to  add file to archive: name={}", item.name(), e);
            throw e;
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
