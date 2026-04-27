package com.waynehays.cloudfilestorage.core.quota;

import com.waynehays.cloudfilestorage.core.quota.dto.SpaceReleaseDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface StorageQuotaServiceApi {

    Page<Long> findAllUserIds(int page, int limit);

    void createStorageQuota(Long userId, long storageLimit);

    void reserveSpace(Long userId, long bytes);

    void releaseSpace(Long userId, long bytes);

    void reconcileUsedSpace(List<Long> userIds);

    void batchReleaseUsedSpace(List<SpaceReleaseDto> spaceRelease);
}
