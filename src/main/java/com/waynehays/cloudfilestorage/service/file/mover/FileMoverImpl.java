package com.waynehays.cloudfilestorage.service.file.mover;

import com.waynehays.cloudfilestorage.dto.file.ResourcePath;
import com.waynehays.cloudfilestorage.dto.file.response.ResourceDto;
import com.waynehays.cloudfilestorage.dto.fileinfo.FileInfoDto;
import com.waynehays.cloudfilestorage.filestorage.FileStorage;
import com.waynehays.cloudfilestorage.mapper.ResourceMapper;
import com.waynehays.cloudfilestorage.parser.resourcepathparser.ResourcePathParser;
import com.waynehays.cloudfilestorage.service.fileinfo.FileInfoService;
import com.waynehays.cloudfilestorage.service.keygenerator.StorageKeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileMoverImpl implements FileMover {
    private static final String MSG_CANNOT_MOVE_DIRECTORY = "Cannot move directory";

    private final FileStorage fileStorage;
    private final FileInfoService fileInfoService;
    private final StorageKeyGenerator storageKeyGenerator;
    private final ResourcePathParser resourcePathParser;
    private final ResourceMapper resourceMapper;

    @Override
    public ResourceDto move(Long userId, String pathFrom, String pathTo) {
        ResourcePath from = resourcePathParser.parse(pathFrom);
        ResourcePath to = resourcePathParser.parse(pathTo);

        if (from.isDirectory() || to.isDirectory()) {
            throw new UnsupportedOperationException(MSG_CANNOT_MOVE_DIRECTORY);
            // TODO: write logic copy directory
        }

        FileInfoDto file = fileInfoService.find(userId, from.directory(), from.filename());
        String newStorageKey = storageKeyGenerator.generate(userId, to.directory(), to.filename());

        fileStorage.move(file.storageKey(), newStorageKey);
        FileInfoDto moved = moveFileInfo(userId, from, to, newStorageKey, file.storageKey());

        return resourceMapper.toDto(moved);
    }

    private FileInfoDto moveFileInfo(Long userId, ResourcePath from, ResourcePath to, String newStorageKey, String oldStorageKey) {
        try {
            return fileInfoService.move(userId, from.directory(), from.filename(), to.directory(), to.filename(), newStorageKey);
        } catch (Exception e) {
            rollbackStorageMove(newStorageKey, oldStorageKey);
            throw e;
        }
    }

    private void rollbackStorageMove(String currentKey, String originalKey) {
        try {
            fileStorage.move(currentKey, originalKey);
        } catch (Exception rollbackEx) {
            log.error("Failed to rollback storage move from {} to {}", currentKey, originalKey, rollbackEx);
        }
    }
}
