package com.waynehays.cloudfilestorage.quota.repository;

import com.waynehays.cloudfilestorage.quota.dto.SpaceReleaseDto;

import java.util.List;

public interface StorageQuotaRepositoryCustom {

    void batchReleaseUsedSpace(List<SpaceReleaseDto> releases);
}
