package com.waynehays.cloudfilestorage.service.resource.upload;

import com.waynehays.cloudfilestorage.service.quota.StorageQuotaServiceApi;
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
    public void rollback(RollbackSnapshot snapshot) {
        if (snapshot.quotaReserved()) {
            quotaService.releaseSpace(snapshot.userId(), snapshot.totalSize());
        }
    }
}
