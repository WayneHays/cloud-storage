package com.waynehays.cloudfilestorage.service.file.downloader;

import com.waynehays.cloudfilestorage.dto.file.ResourcePath;
import com.waynehays.cloudfilestorage.dto.file.response.FileDownloadDto;
import com.waynehays.cloudfilestorage.dto.fileinfo.FileInfoDto;
import com.waynehays.cloudfilestorage.exception.FileNotFoundException;
import com.waynehays.cloudfilestorage.exception.FileStorageException;
import com.waynehays.cloudfilestorage.filestorage.FileStorage;
import com.waynehays.cloudfilestorage.parser.resourcepathparser.ResourcePathParser;
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
    private final ResourcePathParser resourcePathParser;

    @Override
    public FileDownloadDto download(Long userId, String path) {
        ResourcePath resourcePath = resourcePathParser.parse(path);

        if (resourcePath.isDirectory()) {
            return downloadDirectory(userId, resourcePath.directory());
        }
        return downloadFile(userId, resourcePath.directory(), resourcePath.filename());
    }

    private FileDownloadDto downloadDirectory(Long userId, String baseDirectory) {
        List<FileInfoDto> files = fileInfoService.findAllInDirectoryRecursive(userId, baseDirectory);
        byte[] zipBytes = createZipArchive(files, baseDirectory);
        String zipName = PathUtils.extractFilename(baseDirectory) + ".zip";
        return new FileDownloadDto(new ByteArrayInputStream(zipBytes), zipBytes.length, zipName);
    }

    private FileDownloadDto downloadFile(Long userId, String directory, String filename) {
        FileInfoDto file = fileInfoService.find(userId, directory, filename);
        InputStream inputStream = getFileStream(file);
        return new FileDownloadDto(inputStream, file.size(), file.name());
    }

    private byte[] createZipArchive(List<FileInfoDto> files, String baseDirectory) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (FileInfoDto file : files) {
                addFileToZip(zos, file, baseDirectory);
            }

            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new FileStorageException(MSG_ZIP_CREATION_FAILED + baseDirectory, e);
        }
    }

    private void addFileToZip(ZipOutputStream zos, FileInfoDto fileInfo, String baseDirectory) throws IOException {
        String entryPath = buildEntryPath(fileInfo, baseDirectory);
        zos.putNextEntry(new ZipEntry(entryPath));

        try (InputStream is = getFileStream(fileInfo)) {
            is.transferTo(zos);
        }

        zos.closeEntry();
    }

    private InputStream getFileStream(FileInfoDto fileInfo) {
        return fileStorage.get(fileInfo.storageKey())
                .orElseThrow(() -> new FileNotFoundException(
                        MSG_FILE_NOT_FOUND_IN_STORAGE + fileInfo.name()));
    }

    private String buildEntryPath(FileInfoDto fileInfo, String baseDirectory) {
        String fullPath = PathUtils.combine(fileInfo.directory(), fileInfo.name());
        return PathUtils.removePrefix(fullPath, baseDirectory);
    }
}
