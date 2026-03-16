package com.waynehays.cloudfilestorage.component.archiver;

import java.io.OutputStream;
import java.util.List;

public interface ArchiverApi {

    void archiveFiles(List<ArchiveItem> items, OutputStream outputStream);

    String getExtension();
}
