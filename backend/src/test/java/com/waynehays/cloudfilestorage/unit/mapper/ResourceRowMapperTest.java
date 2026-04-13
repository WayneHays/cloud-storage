package com.waynehays.cloudfilestorage.unit.mapper;

import com.waynehays.cloudfilestorage.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.dto.internal.metadata.DirectoryRow;
import com.waynehays.cloudfilestorage.dto.internal.metadata.FileRow;
import com.waynehays.cloudfilestorage.mapper.ResourceRowMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceRowMapperTest {

    private final ResourceRowMapper mapper = Mappers.getMapper(ResourceRowMapper.class);

    @Test
    @DisplayName("Should map path, parent path, name and size")
    void shouldMapPathParentPathNameAndSize() {
        // given
        UploadObjectDto uploadObject = new UploadObjectDto(
                "report.pdf", "report.pdf", "docs/", "docs/report.pdf",
                2048L, "application/pdf", InputStream::nullInputStream);

        // when
        FileRow result = mapper.toFileRow(uploadObject);

        // then
        assertThat(result.path()).isEqualTo("docs/report.pdf");
        assertThat(result.parentPath()).isEqualTo("docs/");
        assertThat(result.name()).isEqualTo("report.pdf");
        assertThat(result.size()).isEqualTo(2048L);
    }

    @Test
    @DisplayName("Should handle root level file")
    void shouldHandleRootLevelFile() {
        // given
        UploadObjectDto uploadObject = new UploadObjectDto(
                "file.txt", "file.txt", "", "file.txt",
                100L, "text/plain", InputStream::nullInputStream);

        // when
        FileRow result = mapper.toFileRow(uploadObject);

        // then
        assertThat(result.path()).isEqualTo("file.txt");
        assertThat(result.parentPath()).isEmpty();
        assertThat(result.name()).isEqualTo("file.txt");
    }

    @Test
    @DisplayName("Should map list")
    void shouldMapList() {
        // given
        UploadObjectDto first = new UploadObjectDto(
                "a.txt", "a.txt", "docs/", "docs/a.txt",
                100L, "text/plain", InputStream::nullInputStream);
        UploadObjectDto second = new UploadObjectDto(
                "b.txt", "b.txt", "docs/", "docs/b.txt",
                200L, "text/plain", InputStream::nullInputStream);

        // when
        List<FileRow> result = mapper.toFileRows(List.of(first, second));

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(FileRow::name)
                .containsExactly("a.txt", "b.txt");
    }

    @Test
    @DisplayName("Should map path, parent path, name")
    void shouldMapPathParentPathAndName() {
        // given
        String path = "docs/reports/";

        // when
        DirectoryRow result = mapper.toDirectoryRow(path);

        // then
        assertThat(result.path()).isEqualTo("docs/reports/");
        assertThat(result.parentPath()).isEqualTo("docs/");
        assertThat(result.name()).isEqualTo("reports/");
    }

    @Test
    @DisplayName("Should handle root level directory")
    void shouldHandleRootLevelDirectory() {
        // given
        String path = "docs/";

        // when
        DirectoryRow result = mapper.toDirectoryRow(path);

        // then
        assertThat(result.path()).isEqualTo("docs/");
        assertThat(result.parentPath()).isEmpty();
        assertThat(result.name()).isEqualTo("docs/");
    }

    @Test
    @DisplayName("Should map set")
    void shouldMapSet() {
        // given
        Set<String> paths = Set.of("docs/", "images/");

        // when
        List<DirectoryRow> result = mapper.toDirectoryRows(paths);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(DirectoryRow::name)
                .containsExactlyInAnyOrder("docs/", "images/");
    }
}
