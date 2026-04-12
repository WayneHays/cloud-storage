package com.waynehays.cloudfilestorage.repository.quota;

import com.waynehays.cloudfilestorage.dto.internal.quota.SpaceCorrectionDto;
import com.waynehays.cloudfilestorage.dto.internal.quota.SpaceReleaseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@RequiredArgsConstructor
public class StorageQuotaRepositoryCustomImpl implements StorageQuotaRepositoryCustom {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void batchUpdateUsedSpace(List<SpaceCorrectionDto> corrections) {
        String sql = """
                UPDATE storage_quotas
                SET used_space = ?
                WHERE user_id = ?
                """;
        List<Object[]> params = corrections.stream()
                .map(c -> new Object[]{c.actualUsedSpace(), c.userId()})
                .toList();
        jdbcTemplate.batchUpdate(sql, params);
    }

    @Override
    public void batchDecreaseUsedSpace(List<SpaceReleaseDto> releases) {
        String sql = """
                UPDATE storage_quotas
                SET used_space = GREATEST(0, used_space - ?)
                WHERE user_id = ?
                """;
        List<Object[]> params = releases.stream()
                .map(r -> new Object[]{r.bytes(), r.userId()})
                .toList();
        jdbcTemplate.batchUpdate(sql, params);
    }
}
