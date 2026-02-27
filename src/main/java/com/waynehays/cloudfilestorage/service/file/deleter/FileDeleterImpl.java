package com.waynehays.cloudfilestorage.service.file.deleter;

import com.waynehays.cloudfilestorage.dto.file.ResourcePath;
import com.waynehays.cloudfilestorage.exception.FileNotFoundException;
import com.waynehays.cloudfilestorage.filestorage.FileStorage;
import com.waynehays.cloudfilestorage.parser.resourcepathparser.ResourcePathParser;
import com.waynehays.cloudfilestorage.service.fileinfo.FileInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileDeleterImpl implements FileDeleter {
    private final FileStorage fileStorage;
    private final FileInfoService fileInfoService;
    private final ResourcePathParser resourcePathParser;

    @Override
    public void delete(Long userId, String path) throws FileNotFoundException {
        ResourcePath resourcePath = resourcePathParser.parse(path);

        if (resourcePath.isDirectory()) {
            // TODO: recursive deletion directory
        }
        String storageKey = fileInfoService.deleteAndReturnStorageKey(userId, resourcePath.directory(), resourcePath.filename());
        fileStorage.delete(storageKey);
    }
}
