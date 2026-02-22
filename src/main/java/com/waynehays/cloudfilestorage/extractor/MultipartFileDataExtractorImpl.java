package com.waynehays.cloudfilestorage.extractor;

import com.waynehays.cloudfilestorage.constant.Constants;
import com.waynehays.cloudfilestorage.dto.files.FileData;
import com.waynehays.cloudfilestorage.exception.FileStorageException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Component
public class MultipartFileDataExtractorImpl implements MultipartFileDataExtractor {
    private static final String MSG_FAILED_READ_FILE = "Failed to read file stream";
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final String ROOT_DIRECTORY = "";

    @Override
    public FileData extract(MultipartFile file, String directory) {
        String originalFilename = file.getOriginalFilename();
        String normalized = FilenameUtils.separatorsToUnix(originalFilename);
        String filename = FilenameUtils.getName(normalized);
        String extension = FilenameUtils.getExtension(filename);

        String embeddedPath = FilenameUtils.getPath(normalized);
        String finalDirectory = mergeDirectories(directory, embeddedPath);

        long size = file.getSize();
        String contentType = extractContentType(file);
        InputStream inputStream = extractInputStream(file);

        return FileData.builder()
                .originalFilename(originalFilename)
                .filename(filename)
                .directory(finalDirectory)
                .extension(extension)
                .size(size)
                .contentType(contentType)
                .inputStream(inputStream)
                .build();
    }

    private String mergeDirectories(String baseDirectory, String subDirectory) {
        String normalizedBase = normalizeDirectory(baseDirectory);
        String normalizedSub = normalizeDirectory(subDirectory);
        return combineDirectories(normalizedBase, normalizedSub);
    }

    private InputStream extractInputStream(MultipartFile file) {
        try {
            return file.getInputStream();
        } catch (IOException e) {
            throw new FileStorageException(MSG_FAILED_READ_FILE, e);
        }
    }

    private String extractContentType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null ? contentType : DEFAULT_CONTENT_TYPE;
    }

    private String normalizeDirectory(String directory) {
        if (directory == null) {
            return "";
        }
        String trimmed = directory.trim();
        String normalized = FilenameUtils.separatorsToUnix(trimmed);
        return StringUtils.strip(normalized, Constants.PATH_SEPARATOR);
    }

    private String combineDirectories(String baseDirectory, String subDirectory) {
        if (baseDirectory.isEmpty() && subDirectory.isEmpty()) {
            return ROOT_DIRECTORY;
        }
        if (baseDirectory.isEmpty()) {
            return subDirectory;
        }
        if (subDirectory.isEmpty()) {
            return baseDirectory;
        }
        return baseDirectory + Constants.PATH_SEPARATOR + subDirectory;
    }
}
