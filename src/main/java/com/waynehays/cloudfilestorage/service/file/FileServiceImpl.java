package com.waynehays.cloudfilestorage.service.file;

import com.waynehays.cloudfilestorage.dto.files.response.FileDownloadDto;
import com.waynehays.cloudfilestorage.dto.files.response.ResourceDto;
import com.waynehays.cloudfilestorage.service.file.deleter.FileDeleter;
import com.waynehays.cloudfilestorage.service.file.downloader.FileDownloader;
import com.waynehays.cloudfilestorage.service.file.uploader.FileUploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
    private final FileUploader fileUploader;
    private final FileDeleter fileDeleter;
    private final FileDownloader fileDownloader;

    @Override
    public ResourceDto uploadFile(Long userId, String directory, MultipartFile file) {
        return fileUploader.uploadFile(userId, directory, file);
    }

    @Override
    public void deleteFile(Long userId, String directory, String filename) {
        fileDeleter.delete(userId, directory, filename);
    }

    @Override
    public FileDownloadDto downloadFile(Long userId, String path) {
        return fileDownloader.download(userId, path);
    }

    @Override
    public ResourceDto moveFile(Long userId, String sourceDirectory, String targetDirectory) {
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
