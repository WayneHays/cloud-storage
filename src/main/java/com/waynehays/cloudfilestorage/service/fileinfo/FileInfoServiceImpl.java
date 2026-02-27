package com.waynehays.cloudfilestorage.service.fileinfo;

import com.waynehays.cloudfilestorage.constant.Constants;
import com.waynehays.cloudfilestorage.dto.file.FileData;
import com.waynehays.cloudfilestorage.dto.fileinfo.FileInfoDto;
import com.waynehays.cloudfilestorage.entity.FileInfo;
import com.waynehays.cloudfilestorage.entity.User;
import com.waynehays.cloudfilestorage.exception.FileAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.FileNotFoundException;
import com.waynehays.cloudfilestorage.mapper.FileInfoMapper;
import com.waynehays.cloudfilestorage.repository.FileInfoRepository;
import com.waynehays.cloudfilestorage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FileInfoServiceImpl implements FileInfoService {
    private static final String MSG_FILE_NOT_FOUND = "File not found: ";
    private static final String MSG_FILE_ALREADY_EXISTS = "File already exists: ";

    private final FileInfoRepository fileInfoRepository;
    private final UserRepository userRepository;
    private final FileInfoMapper fileInfoMapper;

    @Override
    @Transactional
    public FileInfoDto save(Long userId, FileData fileData, String storageKey) {
        User user = userRepository.getReferenceById(userId);
        FileInfo fileInfo = FileInfo.builder()
                .directory(fileData.directory())
                .name(fileData.filename())
                .storageKey(storageKey)
                .size(fileData.size())
                .contentType(fileData.contentType())
                .user(user)
                .build();

        FileInfo saved = saveOrThrow(fileInfo);
        return fileInfoMapper.toDto(saved);
    }

    @Override
    public FileInfoDto find(Long userId, String directory, String filename) {
        FileInfo fileInfo = findEntity(userId, directory, filename);
        return fileInfoMapper.toDto(fileInfo);
    }

    @Override
    @Transactional
    public void delete(Long userId, String directory, String filename) {
        fileInfoRepository.deleteByUserIdAndDirectoryAndName(userId, directory, filename);
    }

    @Override
    @Transactional
    public String deleteAndReturnStorageKey(Long userId, String directory, String filename) {
        return fileInfoRepository.deleteAndReturnStorageKey(userId, directory, filename)
                .orElseThrow(() -> fileNotFound(directory, filename));
    }

    @Override
    @Transactional
    public FileInfoDto move(Long userId, String directory, String filename,
                            String newDirectory, String newFilename, String newStorageKey) {
        FileInfo fileInfo = findEntity(userId, directory, filename);
        fileInfo.setDirectory(newDirectory);
        fileInfo.setName(newFilename);
        fileInfo.setStorageKey(newStorageKey);

        FileInfo saved = saveOrThrow(fileInfo);
        return fileInfoMapper.toDto(saved);
    }

    @Override
    public List<FileInfoDto> searchByName(Long userId, String name) {

        List<FileInfo> files = fileInfoRepository.findByUserIdAndNameContainingIgnoreCase(userId, name);
        return toDtoList(files);
    }

    @Override
    public List<FileInfoDto> findAllInDirectoryRecursive(Long userId, String directory) {
        List<FileInfo> files;

        if (directory.isEmpty()) {
            files = fileInfoRepository.findByUserId(userId);
            return toDtoList(files);
        } else {
            files = fileInfoRepository.findByUserIdAndDirectoryRecursive(userId, directory);
        }

        return toDtoList(files);
    }

    private FileInfo findEntity(Long userId, String directory, String filename) {
        return fileInfoRepository.findByUserIdAndDirectoryAndName(userId, directory, filename)
                .orElseThrow(() -> fileNotFound(directory, filename));
    }

    private FileInfo saveOrThrow(FileInfo fileInfo) {
        try {
            return fileInfoRepository.saveAndFlush(fileInfo);
        } catch (DataIntegrityViolationException e) {
            throw fileAlreadyExists(fileInfo.getDirectory(), fileInfo.getName());
        }
    }

    private List<FileInfoDto> toDtoList(List<FileInfo> files) {
        return fileInfoMapper.toDtoList(files);
    }

    private FileNotFoundException fileNotFound(String directory, String filename) {
        return new FileNotFoundException(MSG_FILE_NOT_FOUND + directory + Constants.PATH_SEPARATOR + filename);
    }

    private FileAlreadyExistsException fileAlreadyExists(String directory, String filename) {
        return new FileAlreadyExistsException(MSG_FILE_ALREADY_EXISTS + directory + Constants.PATH_SEPARATOR + filename);
    }
}
