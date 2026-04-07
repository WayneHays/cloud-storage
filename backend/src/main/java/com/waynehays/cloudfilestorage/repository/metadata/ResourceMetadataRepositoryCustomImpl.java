package com.waynehays.cloudfilestorage.repository.metadata;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@RequiredArgsConstructor
public class ResourceMetadataRepositoryCustomImpl implements ResourceMetadataRepositoryCustom {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void saveDirectoriesIfNotExist(List<Object[]> params) {
        String sql = """
            INSERT INTO resource_metadata (user_id, path, parent_path, name, type, size, marked_for_deletion)
            VALUES (?, ?, ?, ?, 'DIRECTORY', 0, false)
            ON CONFLICT (user_id, path) DO NOTHING
            """;

        jdbcTemplate.batchUpdate(sql, params);
    }
}
