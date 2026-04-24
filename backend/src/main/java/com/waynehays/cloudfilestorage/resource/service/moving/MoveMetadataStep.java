package com.waynehays.cloudfilestorage.resource.service.moving;

import com.waynehays.cloudfilestorage.resource.service.metadata.ResourceMetadataServiceApi;
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
