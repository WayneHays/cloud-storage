package com.waynehays.cloudfilestorage.service.fileservice.uploader;

import com.waynehays.cloudfilestorage.constant.Constants;
import com.waynehays.cloudfilestorage.dto.files.FileData;
import com.waynehays.cloudfilestorage.dto.files.response.ResourceDto;
import com.waynehays.cloudfilestorage.entity.FileInfo;
import com.waynehays.cloudfilestorage.entity.User;
import com.waynehays.cloudfilestorage.exception.FileAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.FileStorageException;
import com.waynehays.cloudfilestorage.filestorage.FileStorage;
import com.waynehays.cloudfilestorage.mapper.FileInfoMapper;
import com.waynehays.cloudfilestorage.parser.multipartfiledataparser.MultipartFileDataParser;
import com.waynehays.cloudfilestorage.repository.FileInfoRepository;
import com.waynehays.cloudfilestorage.repository.UserRepository;
import com.waynehays.cloudfilestorage.validator.PathValidator;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUploaderImpl implements FileUploader {
    private static final String MSG_FILE_ALREADY_EXISTS = "File already exists: ";
    private static final String MSG_SAVING_TO_STORAGE_FAILED = "Failed to save file to storage: ";

    private final FileStorage fileStorage;
    private final PathValidator pathValidator;
    private final MultipartFileDataParser extractor;
    private final UserRepository userRepository;
    private final FileInfoRepository fileInfoRepository;
    private final FileInfoMapper fileInfoMapper;

    @Override
    public ResourceDto uploadFile(Long userId, String directory, MultipartFile file) {
        pathValidator.validateUploadPath(file.getOriginalFilename(), directory);

        FileData fileData = extractor.extract(file, directory);
        User user = userRepository.getReferenceById(userId);
        String storageKey = generateStorageKey(userId, fileData.directory(), fileData.extension());
        FileInfo fileInfo = createFileInfo(fileData, storageKey, user);

        FileInfo saved = trySaveToDatabase(fileInfo);
        trySaveToStorage(fileData, storageKey, saved);

        return fileInfoMapper.toResourceDto(saved);
    }

    private FileInfo trySaveToDatabase(FileInfo fileInfo) {
        FileInfo saved;
        try {
            saved = fileInfoRepository.save(fileInfo);
        } catch (DataIntegrityViolationException e) {
            throw new FileAlreadyExistsException(MSG_FILE_ALREADY_EXISTS + fileInfo.getName());
        }
        return saved;
    }

    private void trySaveToStorage(FileData fileData, String storageKey, FileInfo saved) {
        try {
            fileStorage.put(fileData.inputStream(), storageKey, fileData.size(), fileData.contentType());
        } catch (Exception e) {
            fileInfoRepository.delete(saved);
            throw new FileStorageException(MSG_SAVING_TO_STORAGE_FAILED + fileData.filename(), e);
        }
    }

    private String generateStorageKey(Long userId, String directory, String extension) {
        String uuid = UUID.randomUUID().toString();

        StringBuilder key = new StringBuilder();
        key.append(userId);

        if (StringUtils.isNotEmpty(directory)) {
            key.append(Constants.PATH_SEPARATOR).append(directory);
        }

        key.append(Constants.PATH_SEPARATOR).append(uuid);

        if (StringUtils.isNotEmpty(extension)) {
            key.append(Constants.EXTENSION_SEPARATOR).append(extension);
        }

        return key.toString();
    }

    private FileInfo createFileInfo(FileData fileData, String storageKey, User user) {
        return FileInfo.builder()
                .directory(fileData.directory())
                .name(fileData.filename())
                .storageKey(storageKey)
                .size(fileData.size())
                .contentType(fileData.contentType())
                .user(user)
                .build();
    }
}
