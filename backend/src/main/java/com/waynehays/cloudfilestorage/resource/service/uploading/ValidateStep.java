package com.waynehays.cloudfilestorage.resource.service.uploading;

import com.waynehays.cloudfilestorage.resource.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.shared.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.resource.service.ResourceMetadataServiceApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
class ValidateStep implements UploadStep {
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public void execute(UploadContext context) {
        Long userId = context.getUserId();
        List<String> paths = context.getObjects()
                .stream()
                .map(UploadObjectDto::fullPath)
                .toList();

        Set<String> existing = metadataService.findExistingPaths(userId, new HashSet<>(paths));

        if (!existing.isEmpty()) {
            throw new ResourceAlreadyExistsException("Resources already exist", new ArrayList<>(existing));
        }
    }
}
