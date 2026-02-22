package com.waynehays.cloudfilestorage.service.fileservice;

import com.waynehays.cloudfilestorage.dto.files.response.ResourceDto;
import com.waynehays.cloudfilestorage.service.fileservice.fileuploader.FileUploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
    private final FileUploader fileUploader;

    @Override
    public ResourceDto uploadFile(Long userId, String directory, MultipartFile file) {
        return fileUploader.uploadFile(userId, directory, file);
    }

    @Override
    public void deleteFile(Long userId, String directory, String filename) {

    }

    @Override
    public ResourceDto moveFile(Long userId, String sourceDirectory, String targetDirectory) {
        return null;
    }

    @Override
    public InputStream downloadFile(Long userId, String directory, String filename) {
        return null;
    }

    @Override
    public ResourceDto getFileInfo(Long userId, String directory, String filename) {
        return null;
    }

    @Override
    public List<ResourceDto> searchFiles(Long userId, String query) {
        return List.of();
    }
}
