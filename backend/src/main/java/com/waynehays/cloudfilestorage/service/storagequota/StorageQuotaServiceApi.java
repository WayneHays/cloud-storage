package com.waynehays.cloudfilestorage.service.storagequota;

public interface StorageQuotaServiceApi {

    void reserveSpace(Long userId, long bytes);

    void releaseSpace(Long userId, long bytes);

    void correctUsedSpace(Long userId, long actualUsedSpace);
}
