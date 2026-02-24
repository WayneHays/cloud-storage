package com.waynehays.cloudfilestorage.parser.multipartfiledataparser;

import com.waynehays.cloudfilestorage.constant.Constants;
import com.waynehays.cloudfilestorage.dto.files.FileData;
import com.waynehays.cloudfilestorage.exception.FileStorageException;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Component
public class MultipartFileDataParserImpl implements MultipartFileDataParser {
    private static final String MSG_FAILED_READ_FILE = "Failed to read file stream";
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    ;

    @Override
    public FileData parse(MultipartFile file, String directory) {
        String originalFilename = file.getOriginalFilename();
        String normalized = PathUtils.normalizeSeparators(originalFilename);
        String filename = PathUtils.extractFilename(normalized);
        String extension = FilenameUtils.getExtension(filename);

        String embeddedPath = PathUtils.extractParentPath(normalized);
        String finalDirectory = mergeDirectories(directory, embeddedPath);

        return FileData.builder()
                .originalFilename(originalFilename)
                .filename(filename)
                .directory(finalDirectory)
                .extension(extension)
                .size(file.getSize())
                .contentType(resolveContentType(file))
                .inputStream(openInputStream(file))
                .build();
    }

    private String mergeDirectories(String baseDirectory, String subDirectory) {
        String normalizedBase = normalizeDirectory(baseDirectory);
        String normalizedSub = normalizeDirectory(subDirectory);
        return PathUtils.combine(normalizedBase, normalizedSub);
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null ? contentType : DEFAULT_CONTENT_TYPE;
    }

    private InputStream openInputStream(MultipartFile file) {
        try {
            return file.getInputStream();
        } catch (IOException e) {
            throw new FileStorageException(MSG_FAILED_READ_FILE, e);
        }
    }

    private String normalizeDirectory(String directory) {
        if (directory == null) {
            return Constants.ROOT_DIRECTORY;
        }
        String normalized = PathUtils.normalizeSeparators(directory.trim());
        return PathUtils.stripSeparators(normalized);
    }
}
