package com.waynehays.cloudfilestorage.core.metadata;

import com.waynehays.cloudfilestorage.core.metadata.dto.DeleteDirectoryResult;
import com.waynehays.cloudfilestorage.core.metadata.dto.DirectoryRowDto;
import com.waynehays.cloudfilestorage.core.metadata.dto.FileRowDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
class ResourceMetadataRepositoryCustomImpl implements ResourceMetadataRepositoryCustom {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Set<String> findMissingPaths(Long userId, Set<String> paths) {
        String sql = """
                SELECT candidate
                FROM unnest(?) AS candidate
                WHERE NOT EXISTS (
                    SELECT 1 FROM resource_metadata r
                    WHERE r.user_id = ?
                    AND r.normalized_path = LOWER(candidate)
                    AND r.marked_for_deletion = false
                )
                """;
        String[] pathsArray = paths.toArray(new String[0]);
        List<String> result = jdbcTemplate.query(
                sql,
                ps -> {
                    ps.setArray(1, ps.getConnection().createArrayOf("text", pathsArray));
                    ps.setLong(2, userId);
                },
                (rs, rowNum) -> rs.getString("candidate")
        );
        return new HashSet<>(result);
    }

    @Override
    public void batchSaveDirectories(Long userId, List<DirectoryRowDto> directories) {
        String sql = """
                INSERT INTO resource_metadata
                    (user_id, path, normalized_path, parent_path, name, type, size, marked_for_deletion)
                VALUES (?, ?, ?, ?, ?, 'DIRECTORY', 0, false)
                ON CONFLICT (user_id, normalized_path) DO NOTHING
                """;
        List<Object[]> params = directories.stream()
                .map(d -> new Object[]{
                        userId,
                        d.path(),
                        d.normalizedPath(),
                        d.parentPath(),
                        d.name()
                })
                .toList();

        jdbcTemplate.batchUpdate(sql, params);
    }


    @Override
    public void batchSaveFiles(Long userId, List<FileRowDto> files) {
        String sql = """
                INSERT INTO resource_metadata
                    (user_id, storage_key, path, normalized_path, parent_path, name, size, type, marked_for_deletion)
                VALUES (?, ?, ?, ?, ?, ?, ?,'FILE', false)
                """;
        List<Object[]> params = files.stream()
                .map(f -> new Object[]{
                        userId,
                        f.storageKey(),
                        f.path(),
                        f.normalizedPath(),
                        f.parentPath(),
                        f.name(),
                        f.size(),
                })
                .toList();
        jdbcTemplate.batchUpdate(sql, params);
    }

    @Override
    public DeleteDirectoryResult markFilesForDeletionAndCollectKeys(Long userId, String normalizedPrefix) {
        String sql = """
            WITH marked AS (
                UPDATE resource_metadata
                SET marked_for_deletion = true
                WHERE user_id = ?
                AND normalized_path LIKE ?
                AND type = 'FILE'
                RETURNING size, storage_key
            )
            SELECT COALESCE(SUM(size), 0) AS total_size,
                   ARRAY_AGG(storage_key) FILTER (WHERE storage_key IS NOT NULL) AS keys
            FROM marked
            """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            long totalSize = rs.getLong("total_size");
            java.sql.Array sqlArray = rs.getArray("keys");
            List<String> storageKeys = sqlArray != null
                    ? List.of((String[]) sqlArray.getArray())
                    : List.of();
            return new DeleteDirectoryResult(totalSize, storageKeys);
        }, userId, normalizedPrefix + "%");
    }
}
