package com.waynehays.cloudfilestorage.resource.service.uploading;

import com.waynehays.cloudfilestorage.resource.dto.internal.FileRowDto;
import com.waynehays.cloudfilestorage.resource.mapper.BatchInsertMapper;
import com.waynehays.cloudfilestorage.resource.service.ResourceMetadataServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
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
    public void rollback(UploadRollbackSnapshot snapshot) {
        if (snapshot.hasSavedToDbPaths()) {
            metadataService.deleteByPaths(snapshot.userId(), snapshot.savedToDbPaths());
        }
    }
}
