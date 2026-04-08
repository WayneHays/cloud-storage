package com.waynehays.cloudfilestorage.mapper;

import com.waynehays.cloudfilestorage.dto.internal.StorageQuotaDto;
import com.waynehays.cloudfilestorage.entity.StorageQuota;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StorageQuotaMapper {

    StorageQuotaDto toDto(StorageQuota storageQuota);

    default StorageQuota toEntity(Long userId, long storageLimit) {
        StorageQuota quota = new StorageQuota();
        quota.setUserId(userId);
        quota.setUsedSpace(0);
        quota.setStorageLimit(storageLimit);
        return quota;
    }
}
