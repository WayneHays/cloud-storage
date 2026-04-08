package com.waynehays.cloudfilestorage.service.quota;

import com.waynehays.cloudfilestorage.dto.internal.SpaceReleaseDto;
import com.waynehays.cloudfilestorage.dto.internal.StorageQuotaDto;
import com.waynehays.cloudfilestorage.dto.internal.UsedSpaceCorrectionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StorageQuotaServiceApi {

    void createStorageQuota(Long userId, long storageLimit);

    void reserveSpace(Long userId, long bytes);

    void releaseSpace(Long userId, long bytes);

    Page<StorageQuotaDto> findAllQuotas(Pageable pageable);

    void batchUpdateUsedSpace(List<UsedSpaceCorrectionDto> corrections);

    void batchDecreaseUsedSpace(List<SpaceReleaseDto> releases);
}
