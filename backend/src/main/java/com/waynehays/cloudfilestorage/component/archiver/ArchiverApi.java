package com.waynehays.cloudfilestorage.component.archiver;

import com.waynehays.cloudfilestorage.dto.internal.ArchiveItem;

import java.io.OutputStream;
import java.util.List;

public interface ArchiverApi {

    void archiveResources(List<ArchiveItem> items, OutputStream outputStream);

    String getContentType();

    String getExtension();
}
