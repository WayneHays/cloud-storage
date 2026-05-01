package com.waynehays.cloudfilestorage.files.api.resource;

import com.waynehays.cloudfilestorage.files.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.infrastructure.path.PathUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class MultipartFileParser {
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    List<UploadObjectDto> parseAll(List<MultipartFile> files, String directory) {
        return files.stream()
                .map(f -> parse(f, directory))
                .toList();
    }

    private UploadObjectDto parse(MultipartFile file, String directory) {
        String storageKey = UUID.randomUUID().toString();
        String originalFilename = extractOriginalFilename(file);
        String normalizedFilename = PathUtils.normalizeSeparators(originalFilename);
        String filename = PathUtils.extractFilename(normalizedFilename);
        String nestedDirectory = PathUtils.extractParentPath(normalizedFilename);
        String finalDirectory = PathUtils.combine(directory, nestedDirectory);
        String fullPath = PathUtils.combine(finalDirectory, filename);
        String contentType = resolveContentType(file.getContentType());

        return new UploadObjectDto(
                storageKey,
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
            throw new UploadValidationException("Uploaded file has no filename");
        }

        return originalFilename;
    }

    private String resolveContentType(String contentType) {
        return contentType != null
                ? contentType
                : DEFAULT_CONTENT_TYPE;
    }
}
