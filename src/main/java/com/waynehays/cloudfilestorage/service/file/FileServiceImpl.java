package com.waynehays.cloudfilestorage.service.file;

import com.waynehays.cloudfilestorage.dto.file.response.FileDownloadDto;
import com.waynehays.cloudfilestorage.dto.file.response.ResourceDto;
import com.waynehays.cloudfilestorage.dto.fileinfo.FileInfoDto;
import com.waynehays.cloudfilestorage.mapper.ResourceMapper;
import com.waynehays.cloudfilestorage.service.file.deleter.FileDeleter;
import com.waynehays.cloudfilestorage.service.file.downloader.FileDownloader;
import com.waynehays.cloudfilestorage.service.file.mover.FileMover;
import com.waynehays.cloudfilestorage.service.file.uploader.FileUploader;
import com.waynehays.cloudfilestorage.service.fileinfo.FileInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
    private final FileInfoService fileInfoService;
    private final ResourceMapper resourceMapper;
    private final FileUploader fileUploader;
    private final FileDeleter fileDeleter;
    private final FileDownloader fileDownloader;
    private final FileMover fileMover;

    @Override
    public ResourceDto upload(Long userId, String directory, MultipartFile file) {
        return fileUploader.uploadFile(userId, directory, file);
    }

    @Override
    public void delete(Long userId, String path) {
        fileDeleter.delete(userId, path);
    }

    @Override
    public FileDownloadDto download(Long userId, String path) {
        return fileDownloader.download(userId, path);
    }

    @Override
    public ResourceDto move(Long userId, String directoryFrom, String directoryTo) {
        return fileMover.move(userId, directoryFrom, directoryTo);
    }

    @Override
    public List<ResourceDto> search(Long userId, String query) {
        List<FileInfoDto> files = fileInfoService.searchByName(userId, query);
        return files.stream()
                .map(resourceMapper::toDto)
                .toList();
    }

    @Override
    public ResourceDto getInfo(Long userId, String directory, String filename) {
        return null;
    }
}
