package com.waynehays.cloudfilestorage.service.file.uploader;

import com.waynehays.cloudfilestorage.dto.files.FileData;
import com.waynehays.cloudfilestorage.dto.files.response.ResourceDto;
import com.waynehays.cloudfilestorage.entity.FileInfo;
import com.waynehays.cloudfilestorage.filestorage.FileStorage;
import com.waynehays.cloudfilestorage.mapper.FileInfoMapper;
import com.waynehays.cloudfilestorage.parser.multipartfiledataparser.MultipartFileDataParser;
import com.waynehays.cloudfilestorage.service.fileinfo.FileInfoService;
import com.waynehays.cloudfilestorage.service.keygenerator.StorageKeyGenerator;
import com.waynehays.cloudfilestorage.validator.PathValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploaderImpl implements FileUploader {
    private final PathValidator pathValidator;
    private final MultipartFileDataParser multipartFileDataParser;
    private final FileStorage fileStorage;
    private final FileInfoService fileInfoService;
    private final StorageKeyGenerator storageKeyGenerator;
    private final FileInfoMapper fileInfoMapper;

    @Override
    public ResourceDto uploadFile(Long userId, String directory, MultipartFile file) {
        pathValidator.validateUploadPath(file.getOriginalFilename(), directory);

        FileData fileData = multipartFileDataParser.parse(file, directory);
        String storageKey = storageKeyGenerator.generate(userId, fileData.directory(), fileData.filename());

        FileInfo saved = fileInfoService.save(userId, fileData, storageKey);
        trySaveToStorage(fileData, storageKey, saved);

        return fileInfoMapper.toResourceDto(saved);
    }

    private void trySaveToStorage(FileData fileData, String storageKey, FileInfo saved) {
        try {
            fileStorage.put(fileData.inputStream(), storageKey, fileData.size(), fileData.contentType());
        } catch (Exception e) {
            tryDelete(saved);
            throw e;
        }
    }

    private void tryDelete(FileInfo saved) {
        try {
            fileInfoService.deleteFile(saved.getUser().getId(), saved.getDirectory(), saved.getName());
        } catch (Exception rollbackException) {
            log.error("Failed to rollback file info: {}", saved.getName(), rollbackException);
        }
    }
}
