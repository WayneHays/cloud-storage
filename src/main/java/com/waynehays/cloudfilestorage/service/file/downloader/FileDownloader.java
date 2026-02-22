package com.waynehays.cloudfilestorage.service.file.downloader;

import com.waynehays.cloudfilestorage.dto.files.response.FileDownloadDto;
import com.waynehays.cloudfilestorage.exception.FileNotFoundException;

public interface FileDownloader {

    FileDownloadDto download(Long userId, String path) throws FileNotFoundException;
}
