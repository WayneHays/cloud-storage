package com.waynehays.cloudfilestorage.files.operation.upload;

import com.waynehays.cloudfilestorage.core.quota.StorageQuotaServiceApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ReserveQuotaStep implements UploadStep {
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
