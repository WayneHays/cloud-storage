package com.waynehays.cloudfilestorage.core.quota;

import com.waynehays.cloudfilestorage.core.quota.dto.SpaceReleaseDto;

import java.util.List;

interface StorageQuotaRepositoryCustom {

    void batchReleaseUsedSpace(List<SpaceReleaseDto> releases);
}
