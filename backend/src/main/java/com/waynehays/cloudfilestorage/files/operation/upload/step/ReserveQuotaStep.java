package com.waynehays.cloudfilestorage.files.operation.upload.step;

import com.waynehays.cloudfilestorage.core.quota.service.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.files.operation.upload.dto.Context;
import com.waynehays.cloudfilestorage.files.operation.upload.dto.RollbackDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReserveQuotaStep implements UploadStep {
    private final StorageQuotaServiceApi quotaService;

    @Override
    public void execute(Context context) {
        quotaService.reserveSpace(context.getUserId(), context.getTotalSize());
        context.markQuotaReserved();
    }

    @Override
    public void rollback(RollbackDto rollbackDto) {
        quotaService.releaseSpace(rollbackDto.userId(), rollbackDto.totalSize());
    }

    @Override
    public boolean requiresRollback(RollbackDto rollbackDto) {
        return rollbackDto.quotaReserved();
    }
}
