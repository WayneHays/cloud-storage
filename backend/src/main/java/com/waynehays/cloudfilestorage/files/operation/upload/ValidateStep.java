package com.waynehays.cloudfilestorage.files.operation.upload;

import com.waynehays.cloudfilestorage.core.metadata.ResourceMetadataServiceApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class ValidateStep implements UploadStep {
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public void execute(Context context) {
        Long userId = context.getUserId();
        List<String> allPaths = context.getAllPaths();
        metadataService.throwIfAnyExists(userId, allPaths);
        metadataService.throwIfAnyConflictingTypeExists(userId, allPaths);
    }
}
