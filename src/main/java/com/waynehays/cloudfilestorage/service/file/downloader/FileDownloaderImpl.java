package com.waynehays.cloudfilestorage.service.file.downloader;

import com.waynehays.cloudfilestorage.constant.Constants;
import com.waynehays.cloudfilestorage.dto.files.ParsedPath;
import com.waynehays.cloudfilestorage.dto.files.response.FileDownloadDto;
import com.waynehays.cloudfilestorage.entity.FileInfo;
import com.waynehays.cloudfilestorage.exception.FileNotFoundException;
import com.waynehays.cloudfilestorage.exception.FileStorageException;
import com.waynehays.cloudfilestorage.filestorage.FileStorage;
import com.waynehays.cloudfilestorage.parser.querypathparser.QueryPathParser;
import com.waynehays.cloudfilestorage.repository.FileInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class FileDownloaderImpl implements FileDownloader {
    private static final String MSG_FILE_INFO_NOT_FOUND_IN_DATABASE = "File not found: ";
    private static final String MSG_DOWNLOAD_FAILED = "Failed to download file: ";
    private static final String MSG_FILE_NOT_FOUND_IN_STORAGE = "File not found in storage: ";

    private final FileStorage fileStorage;
    private final FileInfoRepository fileInfoRepository;
    private final QueryPathParser queryPathParser;

    @Override
    public FileDownloadDto download(Long userId, String path) throws FileNotFoundException {
        ParsedPath parsedPath = queryPathParser.parse(path);

        if (parsedPath.isDirectory()) {
            throw new UnsupportedOperationException("Directory download not implemented");
            // TODO: write ZIP-download for directories
        }
        return downloadFile(userId, parsedPath.directory(), parsedPath.filename());
    }

    private FileDownloadDto downloadFile(Long userId, String directory, String filename) {
        FileInfo fileInfo = findFileInfo(userId, directory, filename);
        InputStream inputStream = downloadFromStorage(fileInfo.getStorageKey(), filename);

        return new FileDownloadDto(inputStream, fileInfo.getSize());
    }

    private FileInfo findFileInfo(Long userId, String directory, String filename) {
        return fileInfoRepository.findByUserIdAndDirectoryAndName(userId, directory, filename)
                .orElseThrow(() -> new FileNotFoundException(
                        MSG_FILE_INFO_NOT_FOUND_IN_DATABASE + directory + Constants.PATH_SEPARATOR + filename));
    }

    private InputStream downloadFromStorage(String storageKey, String filename) {
        try {
            return fileStorage.get(storageKey)
                    .orElseThrow(() -> new FileNotFoundException(
                            MSG_FILE_NOT_FOUND_IN_STORAGE + filename
                    ));
        } catch (FileNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException(MSG_DOWNLOAD_FAILED + filename, e);
        }
    }
}
