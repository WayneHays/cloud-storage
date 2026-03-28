package com.waynehays.cloudfilestorage.component.validator;

import com.waynehays.cloudfilestorage.dto.ObjectData;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.ResourceStorageLimitException;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.user.UserServiceApi;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class UploadValidator {
    private final UserServiceApi userService;
    private final ResourceMetadataServiceApi metadataService;

    public void validate(Long userId, List<ObjectData> objects) {
        checkStorageLimit(userId, objects);
        checkForDuplicates(userId, objects);
    }

    private void checkStorageLimit(Long userId, List<ObjectData> objects) {
        long uploadSize = objects.stream()
                .mapToLong(ObjectData::size)
                .sum();
        long storageLimit = userService.getUserStorageLimit(userId);
        long usedSpace = metadataService.getUsedSpace(userId);
        long freeSpace = storageLimit - usedSpace;

        if (uploadSize > freeSpace) {
            throw new ResourceStorageLimitException("Not enough storage space", uploadSize, freeSpace);
        }
    }

    private void checkForDuplicates(Long userId, List<ObjectData> objects) {
        List<String> paths = objects.stream()
                .map(ObjectData::fullPath)
                .toList();
        Set<String> seen = new HashSet<>();
        Set<String> duplicates = new HashSet<>();

        for (String path : paths) {
            if (!seen.add(path)) {
                duplicates.add(path);
            }
        }

        if (ObjectUtils.isNotEmpty(duplicates)) {
            throw new ResourceAlreadyExistsException("Duplicate paths in upload request", duplicates.stream().toList());
        }

        metadataService.throwIfAnyExists(userId, paths);
    }
}
