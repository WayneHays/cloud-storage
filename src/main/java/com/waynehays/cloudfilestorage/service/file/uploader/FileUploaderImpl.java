package com.waynehays.cloudfilestorage.service.file.uploader;

import com.waynehays.cloudfilestorage.dto.file.FileData;
import com.waynehays.cloudfilestorage.dto.file.response.ResourceDto;
import com.waynehays.cloudfilestorage.dto.fileinfo.FileInfoDto;
import com.waynehays.cloudfilestorage.filestorage.FileStorage;
import com.waynehays.cloudfilestorage.mapper.ResourceMapper;
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
    private final ResourceMapper resourceMapper;

    @Override
    public ResourceDto uploadFile(Long userId, String directory, MultipartFile file) {
        pathValidator.validateUploadPath(file.getOriginalFilename(), directory);

        FileData fileData = multipartFileDataParser.parse(file, directory);
        String storageKey = storageKeyGenerator.generate(userId, fileData.directory(), fileData.filename());

        FileInfoDto saved = fileInfoService.save(userId, fileData, storageKey);
        saveToStorage(fileData, storageKey, userId, saved);

        return resourceMapper.toDto(saved);
    }

    private void saveToStorage(FileData fileData, String storageKey, Long userId, FileInfoDto saved) {
        try {
            fileStorage.put(fileData.inputStream(), storageKey, fileData.size(), fileData.contentType());
        } catch (Exception e) {
            rollbackSavedFileInfo(userId, saved);
            throw e;
        }
    }

    private void rollbackSavedFileInfo(Long userId, FileInfoDto saved) {
        try {
            fileInfoService.delete(userId, saved.directory(), saved.name());
        } catch (Exception rollbackEx) {
            log.error("Failed to rollback file info: {}", saved.name(), rollbackEx);
        }
    }
}
