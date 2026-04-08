package com.waynehays.cloudfilestorage.repository.metadata;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@RequiredArgsConstructor
public class ResourceMetadataRepositoryCustomImpl implements ResourceMetadataRepositoryCustom {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void saveDirectories(List<Object[]> params) {
        String sql = """
                INSERT INTO resource_metadata (user_id, path, parent_path, name, type, size, marked_for_deletion)
                VALUES (?, ?, ?, ?, 'DIRECTORY', 0, false)
                ON CONFLICT (user_id, path) DO NOTHING
                """;

        jdbcTemplate.batchUpdate(sql, params);
    }

    @Override
    public void saveFiles(List<Object[]> params) {
        String sql = """
                INSERT INTO resource_metadata (user_id, path, parent_path, name, size, type)
                VALUES (?, ?, ?, ?, ?, 'FILE')
                """;
        jdbcTemplate.batchUpdate(sql, params);
    }

    @Override
    public long markForDeletionAndSumSize(Long userId, String prefix) {
        String sql = """
                WITH marked AS (
                    UPDATE resource_metadata
                    SET marked_for_deletion = true
                    WHERE user_id = ?
                    AND path LIKE ?
                    AND type = 'FILE'
                    RETURNING size
                 )
                SELECT COALESCE(SUM(size), 0)
                FROM marked
                """;
        return jdbcTemplate.queryForObject(sql, Long.class, userId, prefix + "%");
    }
}
