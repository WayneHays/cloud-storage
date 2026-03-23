package com.waynehays.cloudfilestorage.component.archiver;

import java.io.OutputStream;
import java.util.List;

public interface ArchiverApi {

    void archiveResources(List<ArchiveItem> items, OutputStream outputStream);

    String getContentType();

    String getExtension();
}
