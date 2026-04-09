package com.waynehays.cloudfilestorage.repository.quota;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@RequiredArgsConstructor
public class StorageQuotaRepositoryCustomImpl implements StorageQuotaRepositoryCustom {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void batchUpdateUsedSpace(List<Object[]> params) {
        String sql = """
                UPDATE storage_quotas
                SET used_space = ?
                WHERE user_id = ?
                """;
        jdbcTemplate.batchUpdate(sql, params);
    }

    @Override
    public void batchDecreaseUsedSpace(List<Object[]> params) {
        String sql = """
                UPDATE storage_quotas
                SET used_space = GREATEST(0, used_space - ?)
                """;
        jdbcTemplate.batchUpdate(sql, params);
    }
}
