package com.waynehays.cloudfilestorage.service.quota;

import com.waynehays.cloudfilestorage.dto.internal.quota.SpaceCorrectionDto;
import com.waynehays.cloudfilestorage.dto.internal.quota.SpaceReleaseDto;
import com.waynehays.cloudfilestorage.dto.internal.quota.StorageQuotaDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface StorageQuotaServiceApi {

    void createStorageQuota(Long userId, long storageLimit);

    void reserveSpace(Long userId, long bytes);

    void releaseSpace(Long userId, long bytes);

    Page<StorageQuotaDto> findAllQuotas(int page, int limit);

    void batchUpdateUsedSpace(List<SpaceCorrectionDto> corrections);

    void batchDecreaseUsedSpace(List<SpaceReleaseDto> releases);
}
