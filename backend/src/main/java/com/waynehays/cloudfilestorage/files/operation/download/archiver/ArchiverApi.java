package com.waynehays.cloudfilestorage.files.operation.download.archiver;

import com.waynehays.cloudfilestorage.files.operation.download.dto.ArchiveItem;

import java.io.OutputStream;
import java.util.List;

public interface ArchiverApi {

    void archiveResources(List<ArchiveItem> items, OutputStream outputStream);

    String getContentType();

    String getExtension();
}
