package com.waynehays.cloudfilestorage.repository.metadata;

import com.waynehays.cloudfilestorage.dto.internal.metadata.DirectoryRowDto;
import com.waynehays.cloudfilestorage.dto.internal.metadata.FileRowDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@RequiredArgsConstructor
public class ResourceMetadataRepositoryCustomImpl implements ResourceMetadataRepositoryCustom {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Set<String> findMissingPaths(Long userId, Set<String> paths) {
        if (paths.isEmpty()) {
            return Set.of();
        }

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
                        d.path().toLowerCase(),
                        d.parentPath().toLowerCase(),
                        d.name()
                })
                .toList();

        jdbcTemplate.batchUpdate(sql, params);
    }

    @Override
    public void batchSaveFiles(Long userId, List<FileRowDto> files) {
        String sql = """
                INSERT INTO resource_metadata
                    (user_id, path, normalized_path, parent_path, name, size, type, marked_for_deletion)
                VALUES (?, ?, ?, ?, ?, ?, 'FILE', false)
                """;
        List<Object[]> params = files.stream()
                .map(f -> new Object[]{
                        userId,
                        f.path(),
                        f.path().toLowerCase(),
                        f.parentPath().toLowerCase(),
                        f.name(),
                        f.size()
                })
                .toList();
        jdbcTemplate.batchUpdate(sql, params);
    }

    @Override
    public long markForDeletionAndSumSize(Long userId, String normalizedPrefix) {
        String sql = """
                WITH marked AS (
                    UPDATE resource_metadata
                    SET marked_for_deletion = true
                    WHERE user_id = ?
                    AND normalized_path LIKE ?
                    AND type = 'FILE'
                    RETURNING size
                )
                SELECT COALESCE(SUM(size), 0)
                FROM marked
                """;
        Long result = jdbcTemplate.queryForObject(sql, Long.class, userId, normalizedPrefix + "%");
        return Objects.requireNonNullElse(result, 0L);
    }
}
