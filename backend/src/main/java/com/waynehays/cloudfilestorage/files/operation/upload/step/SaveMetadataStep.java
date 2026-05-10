package com.waynehays.cloudfilestorage.files.operation.upload.step;

import com.waynehays.cloudfilestorage.core.metadata.dto.CreateFileDto;
import com.waynehays.cloudfilestorage.core.metadata.service.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.files.operation.upload.dto.Context;
import com.waynehays.cloudfilestorage.files.operation.upload.dto.RollbackDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SaveMetadataStep implements UploadStep {
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public void execute(Context context) {
        List<CreateFileDto> files = context.getObjects().stream()
                .map(o -> new CreateFileDto(o.storageKey(), o.fullPath(), o.size()))
                .toList();
        metadataService.saveFiles(context.getUserId(), files);
        context.markAllPathsSavedToDb();
    }

    @Override
    public void rollback(RollbackDto rollbackDto) {
        metadataService.deleteByPaths(rollbackDto.userId(), rollbackDto.savedToDbPaths());
    }

    @Override
    public boolean requiresRollback(RollbackDto rollbackDto) {
        return rollbackDto.hasSavedToDbPaths();
    }
}
