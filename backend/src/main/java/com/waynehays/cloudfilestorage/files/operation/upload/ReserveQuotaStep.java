package com.waynehays.cloudfilestorage.files.operation.upload;

import com.waynehays.cloudfilestorage.core.quota.StorageQuotaServiceApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ReserveQuotaStep implements UploadStep {
    private final StorageQuotaServiceApi quotaService;

    @Override
    public void execute(UploadContext context) {
        quotaService.reserveSpace(context.getUserId(), context.getTotalSize());
        context.markQuotaReserved();
    }

    @Override
    public void rollback(UploadRollbackDto snapshot) {
        if (snapshot.quotaReserved()) {
            quotaService.releaseSpace(snapshot.userId(), snapshot.totalSize());
        }
    }
}
