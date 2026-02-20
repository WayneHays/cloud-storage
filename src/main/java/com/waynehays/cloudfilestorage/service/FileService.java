package com.waynehays.cloudfilestorage.service;

import com.waynehays.cloudfilestorage.dto.files.response.ResourceDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

public interface FileService {

    ResourceDto uploadFile(Long userId, String directory, MultipartFile file);

    void deleteFile(Long userId, String directory, String filename);

    ResourceDto moveFile(Long userId, String sourceDirectory, String targetDirectory);

    InputStream downloadFile(Long userId, String directory, String filename);

    ResourceDto getFileInfo(Long userId, String directory, String filename);

    List<ResourceDto> searchFiles(Long userId, String query);
}
