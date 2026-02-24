package com.waynehays.cloudfilestorage.service.file.mover;

import com.waynehays.cloudfilestorage.dto.files.ParsedPath;
import com.waynehays.cloudfilestorage.dto.files.response.ResourceDto;
import com.waynehays.cloudfilestorage.entity.FileInfo;
import com.waynehays.cloudfilestorage.filestorage.FileStorage;
import com.waynehays.cloudfilestorage.mapper.FileInfoMapper;
import com.waynehays.cloudfilestorage.parser.querypathparser.QueryPathParser;
import com.waynehays.cloudfilestorage.service.fileinfo.FileInfoService;
import com.waynehays.cloudfilestorage.service.keygenerator.StorageKeyGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileMoverImpl implements FileMover {
    private static final String MSG_CANNOT_MOVE_DIRECTORY = "Cannot move directory";

    private final FileStorage fileStorage;
    private final FileInfoService fileInfoService;
    private final StorageKeyGenerator storageKeyGenerator;
    private final QueryPathParser queryPathParser;
    private final FileInfoMapper fileInfoMapper;

    @Override
    public ResourceDto move(Long userId, String directoryFrom, String directoryTo) {
        ParsedPath from = queryPathParser.parse(directoryFrom);
        ParsedPath to = queryPathParser.parse(directoryTo);

        if (from.isDirectory() || to.isDirectory()) {
            throw new UnsupportedOperationException(MSG_CANNOT_MOVE_DIRECTORY);
            // TODO: write logic copy directory
        }

        FileInfo fileInfo = fileInfoService.findFileInfo(userId, from.directory(), from.filename());

        String newStorageKey = storageKeyGenerator.generate(userId, to.directory(), to.filename());

        fileStorage.move(fileInfo.getStorageKey(), newStorageKey);
        FileInfo moved = fileInfoService.moveFileInfo(
                userId,
                from.directory(),
                from.filename(),
                to.directory(),
                to.filename(),
                newStorageKey);

        return fileInfoMapper.toResourceDto(moved);
    }
}
