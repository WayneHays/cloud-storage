package com.waynehays.cloudfilestorage.service.file;

import com.waynehays.cloudfilestorage.dto.files.response.FileDownloadDto;
import com.waynehays.cloudfilestorage.dto.files.response.ResourceDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {

    ResourceDto upload(Long userId, String directory, MultipartFile file);

    FileDownloadDto download(Long userId, String path);

    void delete(Long userId, String path);

    ResourceDto move(Long userId, String sourceDirectory, String targetDirectory);

    List<ResourceDto> search(Long userId, String query);

    ResourceDto getInfo(Long userId, String directory, String filename);
}
