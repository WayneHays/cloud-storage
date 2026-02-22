package com.waynehays.cloudfilestorage.service;

import com.waynehays.cloudfilestorage.constant.Constants;
import com.waynehays.cloudfilestorage.dto.files.FileData;
import com.waynehays.cloudfilestorage.dto.files.response.ResourceDto;
import com.waynehays.cloudfilestorage.entity.FileInfo;
import com.waynehays.cloudfilestorage.entity.User;
import com.waynehays.cloudfilestorage.exception.EmptyFileException;
import com.waynehays.cloudfilestorage.exception.FileAlreadyExistsException;
import com.waynehays.cloudfilestorage.extractor.MultipartFileDataExtractor;
import com.waynehays.cloudfilestorage.filestorage.FileStorage;
import com.waynehays.cloudfilestorage.mapper.FileInfoMapper;
import com.waynehays.cloudfilestorage.repository.FileInfoRepository;
import com.waynehays.cloudfilestorage.repository.UserRepository;
import com.waynehays.cloudfilestorage.validator.UploadPathValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
    private static final String MSG_FILE_ALREADY_EXISTS = "File already exists: ";

    private final FileStorage fileStorage;
    private final FileInfoRepository fileInfoRepository;
    private final UserRepository userRepository;
    private final UploadPathValidator uploadPathValidator;
    private final MultipartFileDataExtractor multipartFileDataExtractor;
    private final FileInfoMapper fileInfoMapper;

    @Override
    public ResourceDto uploadFile(Long userId, String directory, MultipartFile file) {
        if (file.isEmpty()) {
            throw new EmptyFileException("File to upload is empty");
        }

        String originalFilename = file.getOriginalFilename();
        uploadPathValidator.validate(originalFilename, directory);

        FileData fileData = multipartFileDataExtractor.extract(file, directory);
        User user = userRepository.getReferenceById(userId);

        String storageKey = generateStorageKey(userId, fileData.directory(), fileData.extension());
        fileStorage.put(fileData.inputStream(), storageKey, fileData.size(), fileData.contentType());

        FileInfo fileInfo = createFileInfo(fileData, storageKey, user);

        try {
            FileInfo saved = fileInfoRepository.save(fileInfo);
            return fileInfoMapper.toResourceDto(saved);
        } catch (DataIntegrityViolationException e) {
            fileStorage.delete(storageKey);
            throw new FileAlreadyExistsException(MSG_FILE_ALREADY_EXISTS + fileData.filename());
        }
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
