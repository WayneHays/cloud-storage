package com.waynehays.cloudfilestorage.core.quota;

import com.waynehays.cloudfilestorage.core.quota.dto.SpaceReleaseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@RequiredArgsConstructor
class StorageQuotaRepositoryCustomImpl implements StorageQuotaRepositoryCustom {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void batchReleaseUsedSpace(List<SpaceReleaseDto> spaceRelease) {
        String sql = """
                UPDATE storage_quotas
                SET used_space = GREATEST(0, used_space - ?)
                WHERE user_id = ?
                """;
        List<Object[]> params = spaceRelease.stream()
                .map(r -> new Object[]{r.bytesToRelease(), r.userId()})
                .toList();
        jdbcTemplate.batchUpdate(sql, params);
    }
}
