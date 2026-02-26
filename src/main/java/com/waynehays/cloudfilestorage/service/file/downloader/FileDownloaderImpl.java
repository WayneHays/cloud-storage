package com.waynehays.cloudfilestorage.service.file.downloader;

import com.waynehays.cloudfilestorage.dto.files.ParsedPath;
import com.waynehays.cloudfilestorage.dto.files.response.FileDownloadDto;
import com.waynehays.cloudfilestorage.entity.FileInfo;
import com.waynehays.cloudfilestorage.exception.FileNotFoundException;
import com.waynehays.cloudfilestorage.exception.FileStorageException;
import com.waynehays.cloudfilestorage.filestorage.FileStorage;
import com.waynehays.cloudfilestorage.parser.querypathparser.QueryPathParser;
import com.waynehays.cloudfilestorage.service.fileinfo.FileInfoService;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileDownloaderImpl implements FileDownloader {
    private static final String MSG_FILE_NOT_FOUND_IN_STORAGE = "File not found in storage: ";
    private static final String MSG_ZIP_CREATION_FAILED = "Failed to create zip archive for directory: ";

    private final FileStorage fileStorage;
    private final FileInfoService fileInfoService;
    private final QueryPathParser queryPathParser;

    @Override
    public FileDownloadDto download(Long userId, String queryPath) {
        ParsedPath parsedPath = queryPathParser.parse(queryPath);

        if (parsedPath.isDirectory()) {
            return downloadDirectory(userId, parsedPath.directory());
        }
        return downloadFile(userId, parsedPath.directory(), parsedPath.filename());
    }

    private FileDownloadDto downloadDirectory(Long userId, String baseDirectory) {
        List<FileInfo> files = fileInfoService.findAllInDirectoryRecursive(userId, baseDirectory);
        byte[] zipBytes = createZipArchive(files, baseDirectory);
        String zipName = PathUtils.extractFilename(baseDirectory) + ".zip";
        return new FileDownloadDto(new ByteArrayInputStream(zipBytes), zipBytes.length, zipName);
    }

    private FileDownloadDto downloadFile(Long userId, String directory, String filename) {
        FileInfo fileInfo = fileInfoService.find(userId, directory, filename);
        InputStream inputStream = getFileStream(fileInfo);
        return new FileDownloadDto(inputStream, fileInfo.getSize(), fileInfo.getName());
    }

    private byte[] createZipArchive(List<FileInfo> files, String baseDirectory) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (FileInfo fileInfo : files) {
                addFileToZip(zos, fileInfo, baseDirectory);
            }

            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new FileStorageException(MSG_ZIP_CREATION_FAILED + baseDirectory, e);
        }
    }

    private void addFileToZip(ZipOutputStream zos, FileInfo fileInfo, String baseDirectory) throws IOException {
        String entryPath = buildEntryPath(fileInfo, baseDirectory);
        zos.putNextEntry(new ZipEntry(entryPath));

        try (InputStream is = getFileStream(fileInfo)) {
            is.transferTo(zos);
        }

        zos.closeEntry();
    }

    private InputStream getFileStream(FileInfo fileInfo) {
        return fileStorage.get(fileInfo.getStorageKey())
                .orElseThrow(() -> new FileNotFoundException(
                        MSG_FILE_NOT_FOUND_IN_STORAGE + fileInfo.getName()));
    }

    private String buildEntryPath(FileInfo fileInfo, String baseDirectory) {
        String fullPath = PathUtils.combine(fileInfo.getDirectory(), fileInfo.getName());
        return PathUtils.removePrefix(fullPath, baseDirectory);
    }
}
