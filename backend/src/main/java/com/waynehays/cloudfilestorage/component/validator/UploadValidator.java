package com.waynehays.cloudfilestorage.component.validator;

import com.waynehays.cloudfilestorage.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class UploadValidator {
    private final ResourceMetadataServiceApi metadataService;

    public void validate(Long userId, List<UploadObjectDto> objects) {
        List<String> paths = objects.stream()
                .map(UploadObjectDto::fullPath)
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
