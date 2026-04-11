package com.waynehays.cloudfilestorage.mapper;

import com.waynehays.cloudfilestorage.dto.internal.quota.StorageQuotaDto;
import com.waynehays.cloudfilestorage.entity.StorageQuota;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StorageQuotaMapper {

    StorageQuotaDto toDto(StorageQuota quota);
}
