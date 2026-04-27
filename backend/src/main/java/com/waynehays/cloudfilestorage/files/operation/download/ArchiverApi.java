package com.waynehays.cloudfilestorage.files.operation.download;

import java.io.OutputStream;
import java.util.List;

interface ArchiverApi {

    void archiveResources(List<ArchiveItem> items, OutputStream outputStream);

    String getContentType();

    String getExtension();
}
