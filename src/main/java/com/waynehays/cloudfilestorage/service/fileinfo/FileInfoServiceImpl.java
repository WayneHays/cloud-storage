package com.waynehays.cloudfilestorage.service.fileinfo;

import com.waynehays.cloudfilestorage.constant.Constants;
import com.waynehays.cloudfilestorage.dto.files.FileData;
import com.waynehays.cloudfilestorage.entity.FileInfo;
import com.waynehays.cloudfilestorage.entity.User;
import com.waynehays.cloudfilestorage.exception.FileAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.FileNotFoundException;
import com.waynehays.cloudfilestorage.repository.FileInfoRepository;
import com.waynehays.cloudfilestorage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FileInfoServiceImpl implements FileInfoService {
    private static final String MSG_FILE_NOT_FOUND = "File not found: ";
    private static final String MSG_FILE_ALREADY_EXISTS = "File already exists: ";

    private final FileInfoRepository fileInfoRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public FileInfo save(Long userId, FileData fileData, String storageKey) {
        User user = userRepository.getReferenceById(userId);
        FileInfo fileInfo = FileInfo.builder()
                .directory(fileData.directory())
                .name(fileData.filename())
                .storageKey(storageKey)
                .size(fileData.size())
                .contentType(fileData.contentType())
                .user(user)
                .build();

        try {
            return fileInfoRepository.save(fileInfo);
        } catch (DataIntegrityViolationException e) {
            throw new FileAlreadyExistsException(
                    MSG_FILE_ALREADY_EXISTS + fileInfo.getDirectory() + Constants.PATH_SEPARATOR + fileInfo.getName()
            );
        }
    }

    @Override
    public FileInfo findFileInfo(Long userId, String directory, String filename) {
        return fileInfoRepository.findByUserIdAndDirectoryAndName(userId, directory, filename)
                .orElseThrow(() -> new FileNotFoundException(
                        MSG_FILE_NOT_FOUND + directory + Constants.PATH_SEPARATOR + filename
                ));
    }

    @Override
    @Transactional
    public void deleteFile(Long userId, String directory, String filename) {
        fileInfoRepository.deleteByUserIdAndDirectoryAndName(userId, directory, filename);
    }

    @Override
    @Transactional
    public String deleteFileInfoAndReturnStorageKey(Long userId, String directory, String filename) {
        FileInfo fileInfo = fileInfoRepository.findByUserIdAndDirectoryAndName(userId, directory, filename)
                .orElseThrow(() -> new FileNotFoundException(MSG_FILE_NOT_FOUND + directory + Constants.PATH_SEPARATOR + filename));
        fileInfoRepository.delete(fileInfo);
        return fileInfo.getStorageKey();
    }

    @Override
    public FileInfo moveFileInfo(Long userId, String directory, String filename, String newDirectory, String newFilename, String newStorageKey) {
        FileInfo fileInfo = findFileInfo(userId, directory, filename);
        fileInfo.setDirectory(newDirectory);
        fileInfo.setName(newFilename);
        fileInfo.setStorageKey(newStorageKey);

        try {
            return fileInfoRepository.save(fileInfo);
        } catch (DataIntegrityViolationException e) {
            throw new FileAlreadyExistsException(MSG_FILE_ALREADY_EXISTS + newDirectory + Constants.PATH_SEPARATOR + filename);
        }
    }
}
