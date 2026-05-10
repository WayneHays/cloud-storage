package com.waynehays.cloudfilestorage.core.quota.service;

import com.waynehays.cloudfilestorage.core.quota.dto.SpaceReleaseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StorageQuotaBatchApi {

    Page<Long> findAllUserIds(Pageable pageable);

    void reconcileUsedSpace(List<Long> userIds);

    void batchReleaseUsedSpace(List<SpaceReleaseDto> spaceToRelease);
}
