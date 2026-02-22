package com.waynehays.cloudfilestorage.service.file;

import com.waynehays.cloudfilestorage.dto.files.response.FileDownloadDto;
import com.waynehays.cloudfilestorage.dto.files.response.ResourceDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {

    ResourceDto uploadFile(Long userId, String directory, MultipartFile file);

    void deleteFile(Long userId, String directory, String filename);

    FileDownloadDto downloadFile(Long userId, String path);

    ResourceDto moveFile(Long userId, String sourceDirectory, String targetDirectory);

    ResourceDto getFileInfo(Long userId, String directory, String filename);

    List<ResourceDto> searchFiles(Long userId, String query);
}
