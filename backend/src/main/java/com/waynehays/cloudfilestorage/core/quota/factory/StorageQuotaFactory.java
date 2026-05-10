package com.waynehays.cloudfilestorage.core.quota.factory;

import com.waynehays.cloudfilestorage.core.quota.entity.StorageQuota;
import org.springframework.stereotype.Component;

@Component
public class StorageQuotaFactory {

    public StorageQuota create(Long userId, long storageLimit) {
        return new StorageQuota(userId, storageLimit);
    }
}
