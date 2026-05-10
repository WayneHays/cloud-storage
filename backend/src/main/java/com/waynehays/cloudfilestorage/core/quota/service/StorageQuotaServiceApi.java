package com.waynehays.cloudfilestorage.core.quota.service;

public interface StorageQuotaServiceApi {

    void createStorageQuota(Long userId, long storageLimit);

    void reserveSpace(Long userId, long bytes);

    void releaseSpace(Long userId, long bytes);
}
