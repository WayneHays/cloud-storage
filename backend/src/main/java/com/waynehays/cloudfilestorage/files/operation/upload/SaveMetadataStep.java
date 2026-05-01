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
    public void execute(UploadContext context) {
        List<FileRowDto> newFiles = batchInsertMapper.toFileRows(context.getObjects());
        metadataService.saveFiles(context.getUserId(), newFiles);
        context.getObjects().forEach(o -> context.addSavedToDbPath(o.fullPath()));
    }

    @Override
    public void rollback(UploadRollbackDto snapshot) {
        metadataService.deleteByPaths(snapshot.userId(), snapshot.savedToDbPaths());
    }

    @Override
    public boolean requiresRollback(UploadRollbackDto snapshot) {
        return snapshot.hasSavedToDbPaths();
    }
}
