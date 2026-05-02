package com.waynehays.cloudfilestorage.files.operation.upload;

import com.waynehays.cloudfilestorage.core.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.core.metadata.dto.FileRowDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class SaveMetadataStep implements UploadStep {
    private final BatchInsertMapper batchInsertMapper;
    private final ResourceMetadataServiceApi metadataService;

    @Override
    public void execute(Context context) {
        List<FileRowDto> newFiles = batchInsertMapper.toFileRows(context.getObjects());
        metadataService.saveFiles(context.getUserId(), newFiles);
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
