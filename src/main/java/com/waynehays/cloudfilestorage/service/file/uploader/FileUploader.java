package com.waynehays.cloudfilestorage.service.file.uploader;

import com.waynehays.cloudfilestorage.dto.file.response.ResourceDto;
import org.springframework.web.multipart.MultipartFile;

public interface FileUploader {
    ResourceDto uploadFile(Long userId, String directory, MultipartFile file);
}
