package com.waynehays.cloudfilestorage.service.resource.downloader;

import com.waynehays.cloudfilestorage.dto.response.DownloadResult;

public interface ResourceDownloaderApi {

    DownloadResult download(Long userId, String path);
}
