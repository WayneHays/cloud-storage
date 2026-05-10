package com.waynehays.cloudfilestorage.files.operation.download;

import com.waynehays.cloudfilestorage.files.operation.download.dto.DownloadResult;

public interface ResourceDownloadServiceApi {

    DownloadResult download(Long userId, String path);
}
