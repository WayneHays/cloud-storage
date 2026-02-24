package com.waynehays.cloudfilestorage.service.file.downloader;

import com.waynehays.cloudfilestorage.dto.files.ParsedPath;
import com.waynehays.cloudfilestorage.dto.files.response.FileDownloadDto;
import com.waynehays.cloudfilestorage.entity.FileInfo;
import com.waynehays.cloudfilestorage.exception.FileNotFoundException;
import com.waynehays.cloudfilestorage.filestorage.FileStorage;
import com.waynehays.cloudfilestorage.parser.querypathparser.QueryPathParser;
import com.waynehays.cloudfilestorage.service.fileinfo.FileInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class FileDownloaderImpl implements FileDownloader {
    private static final String MSG_FILE_NOT_FOUND_IN_STORAGE = "File not found in storage: ";

    private final FileStorage fileStorage;
    private final FileInfoService fileInfoService;
    private final QueryPathParser queryPathParser;

    @Override
    public FileDownloadDto download(Long userId, String path) throws FileNotFoundException {
        ParsedPath parsedPath = queryPathParser.parse(path);

        if (parsedPath.isDirectory()) {
            throw new UnsupportedOperationException("Directory download not implemented");
            // TODO: write ZIP-download for directories
        }
        FileInfo fileInfo = fileInfoService.findFileInfo(userId, parsedPath.directory(), parsedPath.filename());
        InputStream inputStream = downloadFromStorage(fileInfo.getStorageKey(), parsedPath.filename());

        return new FileDownloadDto(inputStream, fileInfo.getSize());
    }

    private InputStream downloadFromStorage(String storageKey, String filename) {
        return fileStorage.get(storageKey)
                .orElseThrow(() -> new FileNotFoundException(
                        MSG_FILE_NOT_FOUND_IN_STORAGE + filename
                ));
    }
}
