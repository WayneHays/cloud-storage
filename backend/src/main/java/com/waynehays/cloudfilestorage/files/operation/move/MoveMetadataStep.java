package com.waynehays.cloudfilestorage.files.operation.move;

import com.waynehays.cloudfilestorage.core.metadata.ResourceMetadataServiceApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class MoveMetadataStep implements MoveStep {
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public void execute(MoveContext context) {
        metadataService.moveMetadata(context.getUserId(), context.getPathFrom(), context.getPathTo());
    }
}
