package com.waynehays.cloudfilestorage.files.operation.upload;

import com.waynehays.cloudfilestorage.core.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.files.dto.internal.UploadObjectDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

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

        metadataService.throwIfAnyExists(userId, paths);
        metadataService.throwIfAnyConflictingTypeExists(userId, paths);
    }
}
