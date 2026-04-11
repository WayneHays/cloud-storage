package com.waynehays.cloudfilestorage.service.resource.download;

import com.waynehays.cloudfilestorage.dto.internal.DownloadResult;

public interface ResourceDownloadServiceApi {

    DownloadResult download(Long userId, String path);
}
