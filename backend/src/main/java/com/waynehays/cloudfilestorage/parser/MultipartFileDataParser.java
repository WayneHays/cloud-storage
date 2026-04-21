package com.waynehays.cloudfilestorage.parser;

import com.waynehays.cloudfilestorage.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.exception.MultipartValidationException;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import com.waynehays.cloudfilestorage.validator.MultipartFileValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class MultipartFileDataParser {
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final MultipartFileValidator validator;

    public UploadObjectDto parse(MultipartFile file, String directory) {
        String originalFilename = extractOriginalFilename(file);
        String normalizedFilename = PathUtils.normalizeSeparators(originalFilename);
        String filename = PathUtils.extractFilename(normalizedFilename);
        String nestedDirectory = PathUtils.extractParentPath(normalizedFilename);
        String finalDirectory = PathUtils.combine(directory, nestedDirectory);
        String fullPath = PathUtils.combine(finalDirectory, filename);
        String contentType = resolveContentType(file.getContentType());

        validator.validate(originalFilename, fullPath, file.getSize());

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

        if (originalFilename == null) {
            throw new MultipartValidationException("Uploaded file has no filename");
        }

        return originalFilename;
    }

    private String resolveContentType(String contentType) {
        return contentType != null ? contentType : DEFAULT_CONTENT_TYPE;
    }
}
