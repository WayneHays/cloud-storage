package com.waynehays.cloudfilestorage.service.file;

import com.waynehays.cloudfilestorage.constant.Constants;
import com.waynehays.cloudfilestorage.dto.file.response.FileDownloadDto;
import com.waynehays.cloudfilestorage.dto.file.response.ResourceDto;
import com.waynehays.cloudfilestorage.dto.fileinfo.FileInfoDto;
import com.waynehays.cloudfilestorage.mapper.ResourceMapper;
import com.waynehays.cloudfilestorage.service.file.deleter.FileDeleter;
import com.waynehays.cloudfilestorage.service.file.downloader.FileDownloader;
import com.waynehays.cloudfilestorage.service.file.mover.FileMover;
import com.waynehays.cloudfilestorage.service.file.uploader.FileUploader;
import com.waynehays.cloudfilestorage.service.fileinfo.FileInfoService;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public List<ResourceDto> search(Long userId, String path) {
        List<FileInfoDto> files = fileInfoService.searchByName(userId, path);
        return files.stream()
                .map(resourceMapper::toDto)
                .toList();
    }

    @Override
    public List<ResourceDto> getDirectoryContent(Long userId, String directory) {
        List<FileInfoDto> directoryContent = fileInfoService.findInDirectory(userId, directory);
        List<ResourceDto> result = new ArrayList<>();
        Set<String> subDirectories = new HashSet<>();

        for (FileInfoDto unit : directoryContent) {
            if (unit.directory().equals(directory)) {
                result.add(resourceMapper.toDto(unit));
            } else {
                subDirectories.add(extractImmediateSubdirectory(unit.directory(), directory));
            }
        }

        for (String subDirectory : subDirectories) {
            result.add(ResourceDto.directory(directory, subDirectory));
        }

        return result;
    }

    private String extractImmediateSubdirectory(String fullDirectory, String baseDirectory) {
        String relative = PathUtils.removePrefix(fullDirectory, baseDirectory);
        int separatorIndex = relative.indexOf(Constants.PATH_SEPARATOR);
        return separatorIndex > 0 ? relative.substring(0, separatorIndex) : relative;
    }
}
