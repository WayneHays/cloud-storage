package com.waynehays.cloudfilestorage.filestorage.minio;

import com.waynehays.cloudfilestorage.filestorage.dto.MetaData;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import io.minio.StatObjectResponse;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MinioConverter {

    public MetaData toMetaData(Item item) {
        boolean isDirectory = isDirectory(item);
        Long size = isDirectory ? null : item.size();
        String name = extractName(item.objectName());

        return MetaData.builder()
                .key(item.objectName())
                .name(name)
                .size(size)
                .isDirectory(isDirectory)
                .build();
    }

    public MetaData toMetaData(StatObjectResponse response) {
        boolean isDirectory = isDirectory(response);
        Long size = isDirectory ? null : response.size();
        String name = extractName(response.object());
        return MetaData.builder()
                .key(response.object())
                .name(name)
                .size(size)
                .contentType(response.contentType())
                .isDirectory(isDirectory)
                .build();
    }

    private boolean isDirectory(Item item) {
        return item.isDir() || PathUtils.isDirectory(item.objectName());
    }

    private boolean isDirectory(StatObjectResponse response) {
        return PathUtils.isDirectory(response.object());
    }

    private String extractName(String objectKey) {
        String cleanKey = PathUtils.removeTrailingSeparator(objectKey);
        return PathUtils.extractFilename(cleanKey);
    }
}
