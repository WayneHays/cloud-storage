package com.waynehays.cloudfilestorage.repository.quota;

import com.waynehays.cloudfilestorage.dto.internal.quota.SpaceCorrectionDto;
import com.waynehays.cloudfilestorage.dto.internal.quota.SpaceReleaseDto;

import java.util.List;

public interface StorageQuotaRepositoryCustom {

    void batchUpdateUsedSpace(List<SpaceCorrectionDto> corrections);

    void batchDecreaseUsedSpace(List<SpaceReleaseDto> releases);
}
