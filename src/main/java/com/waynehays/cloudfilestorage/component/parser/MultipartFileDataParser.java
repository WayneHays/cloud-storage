package com.waynehays.cloudfilestorage.component.parser;

import com.waynehays.cloudfilestorage.dto.ObjectData;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@Component
public class MultipartFileDataParser implements MultipartFileDataParserApi {
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    @Override
    public ObjectData parse(MultipartFile file, String directory) {
        String originalFilename = file.getOriginalFilename();
        String normalizedFilename = PathUtils.normalizeSeparators(originalFilename);
        String filename = PathUtils.extractFilename(normalizedFilename);
        String nestedDirectory = PathUtils.extractParentPath(Objects.requireNonNull(normalizedFilename));
        String finalDirectory = PathUtils.combine(directory, nestedDirectory);
        String fullPath = PathUtils.combine(finalDirectory, filename);

        return new ObjectData(
                originalFilename,
                filename,
                finalDirectory,
                fullPath,
                file.getSize(),
                resolveContentType(file.getContentType()),
                file::getInputStream
        );
    }

    private String resolveContentType(String contentType) {
        return contentType != null ? contentType : DEFAULT_CONTENT_TYPE;
    }
}
