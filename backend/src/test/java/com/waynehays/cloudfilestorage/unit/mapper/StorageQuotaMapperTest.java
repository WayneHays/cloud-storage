package com.waynehays.cloudfilestorage.unit.mapper;

import com.waynehays.cloudfilestorage.dto.internal.quota.StorageQuotaDto;
import com.waynehays.cloudfilestorage.entity.StorageQuota;
import com.waynehays.cloudfilestorage.mapper.StorageQuotaMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class StorageQuotaMapperTest {

    private final StorageQuotaMapper mapper = Mappers.getMapper(StorageQuotaMapper.class);

    @Test
    void toDto_shouldMapAllFields() {
        // given
        StorageQuota quota = new StorageQuota();
        quota.setUserId(1L);
        quota.setUsedSpace(500L);
        quota.setStorageLimit(10000L);

        // when
        StorageQuotaDto result = mapper.toDto(quota);

        // then
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.usedSpace()).isEqualTo(500L);
        assertThat(result.storageLimit()).isEqualTo(10000L);
    }
}
