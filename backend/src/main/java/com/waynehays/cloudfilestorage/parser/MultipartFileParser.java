package com.waynehays.cloudfilestorage.parser;

import com.waynehays.cloudfilestorage.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.exception.MultipartValidationException;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class MultipartFileParser {
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    public UploadObjectDto parse(MultipartFile file, String directory) {
        String originalFilename = extractOriginalFilename(file);
        String normalizedFilename = PathUtils.normalizeSeparators(originalFilename);
        String filename = PathUtils.extractFilename(normalizedFilename);
        String nestedDirectory = PathUtils.extractParentPath(normalizedFilename);
        String finalDirectory = PathUtils.combine(directory, nestedDirectory);
        String fullPath = PathUtils.combine(finalDirectory, filename);
        String contentType = resolveContentType(file.getContentType());

        return new UploadObjectDto(
                originalFilename,
                filename,
                finalDirectory,
                fullPath,
                file.getSize(),
                contentType,
                file::getInputStream
        );
    }

    private String extractOriginalFilename(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();

        if (StringUtils.isBlank(originalFilename)) {
            throw new MultipartValidationException("Uploaded file has no filename");
        }

        return originalFilename;
    }

    private String resolveContentType(String contentType) {
        return contentType != null ? contentType : DEFAULT_CONTENT_TYPE;
    }
}
