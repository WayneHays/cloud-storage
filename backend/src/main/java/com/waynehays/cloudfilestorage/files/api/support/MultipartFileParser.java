package com.waynehays.cloudfilestorage.files.api.support;

import com.waynehays.cloudfilestorage.files.api.exception.MultipartParsingException;
import com.waynehays.cloudfilestorage.files.operation.upload.dto.UploadObjectDto;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MultipartFileParser {
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    public List<UploadObjectDto> parseAll(List<MultipartFile> files, String directory) {
        return files.stream()
                .map(f -> parse(f, directory))
                .toList();
    }

    private UploadObjectDto parse(MultipartFile file, String directory) {
        String storageKey = UUID.randomUUID().toString();
        String originalFilename = extractOriginalFilename(file);
        String normalizedFilename = PathUtils.normalizeSeparators(originalFilename);
        String filename = PathUtils.getName(normalizedFilename);
        String nestedDirectory = PathUtils.getParentPath(normalizedFilename);
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
                file
        );
    }

    private String extractOriginalFilename(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();

        if (StringUtils.isBlank(originalFilename)) {
            throw new MultipartParsingException("Uploaded file has no filename");
        }

        return originalFilename;
    }

    private String resolveContentType(String contentType) {
        return contentType != null
                ? contentType
                : DEFAULT_CONTENT_TYPE;
    }
}
