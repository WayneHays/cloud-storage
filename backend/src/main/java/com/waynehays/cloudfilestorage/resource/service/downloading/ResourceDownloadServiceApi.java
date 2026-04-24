package com.waynehays.cloudfilestorage.resource.service.downloading;

import com.waynehays.cloudfilestorage.resource.dto.internal.DownloadResult;

public interface ResourceDownloadServiceApi {

    DownloadResult download(Long userId, String path);
}
