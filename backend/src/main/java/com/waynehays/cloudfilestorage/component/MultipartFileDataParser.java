package com.waynehays.cloudfilestorage.component;

import com.waynehays.cloudfilestorage.component.validator.MultipartFileValidator;
import com.waynehays.cloudfilestorage.dto.UploadObjectDto;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class MultipartFileDataParser {
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final MultipartFileValidator validator;

    public UploadObjectDto parse(MultipartFile file, String directory) {
        String originalFilename = file.getOriginalFilename();
        String normalizedFilename = PathUtils.normalizeSeparators(originalFilename);
        String filename = PathUtils.extractFilename(normalizedFilename);
        String nestedDirectory = PathUtils.extractParentPath(Objects.requireNonNull(normalizedFilename));
        String finalDirectory = PathUtils.combine(directory, nestedDirectory);
        String fullPath = PathUtils.combine(finalDirectory, filename);
        String contentType = resolveContentType(file.getContentType());

        validator.validate(originalFilename, fullPath);

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

    private String resolveContentType(String contentType) {
        return contentType != null ? contentType : DEFAULT_CONTENT_TYPE;
    }
}
