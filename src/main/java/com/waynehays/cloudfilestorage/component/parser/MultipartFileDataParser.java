package com.waynehays.cloudfilestorage.component.parser;

import com.waynehays.cloudfilestorage.dto.FileData;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@Component
public class MultipartFileDataParser implements MultipartFileDataParserApi {

    @Override
    public FileData parse(MultipartFile file, String directory) {
        String originalFilename = file.getOriginalFilename();
        String normalizedFilename = PathUtils.normalizeSeparators(originalFilename);
        String filename = PathUtils.extractFilename(normalizedFilename);

        String nestedDirectory = PathUtils.extractParentPath(Objects.requireNonNull(normalizedFilename));
        String finalDirectory = PathUtils.combine(directory, nestedDirectory);
        String fullPath = PathUtils.combine(finalDirectory, filename);

        return FileData.builder()
                .originalFilename(originalFilename)
                .filename(filename)
                .directory(finalDirectory)
                .fullPath(fullPath)
                .size(file.getSize())
                .contentType(file.getContentType())
                .inputStreamSupplier(file::getInputStream)
                .build();
    }
}
