package com.waynehays.cloudfilestorage.repository.quota;

import com.waynehays.cloudfilestorage.dto.internal.quota.SpaceReleaseDto;

import java.util.List;

public interface StorageQuotaRepositoryCustom {

    void batchReleaseUsedSpace(List<SpaceReleaseDto> releases);
}
