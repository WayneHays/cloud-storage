package com.waynehays.cloudfilestorage.service.fileservice.uploader;

import com.waynehays.cloudfilestorage.dto.files.response.ResourceDto;
import org.springframework.web.multipart.MultipartFile;

public interface FileUploader {

    ResourceDto uploadFile(Long userId, String directory, MultipartFile file);
}
