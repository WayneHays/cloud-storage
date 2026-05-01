package com.waynehays.cloudfilestorage.files.operation.upload;

import com.waynehays.cloudfilestorage.core.metadata.dto.DirectoryRowDto;
import com.waynehays.cloudfilestorage.core.metadata.dto.FileRowDto;
import com.waynehays.cloudfilestorage.files.dto.internal.UploadObjectDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BatchInsertMapperTest {

    private final BatchInsertMapper mapper = Mappers.getMapper(BatchInsertMapper.class);

    @Test
    @DisplayName("Should map path, parent path, name, size and storageKey")
    void shouldMapAllFields() {
        // given
        UploadObjectDto uploadObject = new UploadObjectDto(
                "key-123",
                "report.pdf", "report.pdf", "docs/", "docs/report.pdf",
                2048L, "application/pdf", InputStream::nullInputStream);

        // when
        FileRowDto result = mapper.toFileRow(uploadObject);

        // then
        assertThat(result.path()).isEqualTo("docs/report.pdf");
        assertThat(result.parentPath()).isEqualTo("docs/");
        assertThat(result.name()).isEqualTo("report.pdf");
        assertThat(result.size()).isEqualTo(2048L);
        assertThat(result.storageKey()).isEqualTo("key-123");
    }

    @Test
    @DisplayName("Should handle root level file")
    void shouldHandleRootLevelFile() {
        // given
        UploadObjectDto uploadObject = new UploadObjectDto(
                "key-456",
                "file.txt", "file.txt", "", "file.txt",
                100L, "text/plain", InputStream::nullInputStream);

        // when
        FileRowDto result = mapper.toFileRow(uploadObject);

        // then
        assertThat(result.path()).isEqualTo("file.txt");
        assertThat(result.parentPath()).isEmpty();
        assertThat(result.name()).isEqualTo("file.txt");
        assertThat(result.storageKey()).isEqualTo("key-456");
    }

    @Test
    @DisplayName("Should map list preserving storageKeys")
    void shouldMapList() {
        // given
        UploadObjectDto first = new UploadObjectDto(
                "key-1",
                "a.txt", "a.txt", "docs/", "docs/a.txt",
                100L, "text/plain", InputStream::nullInputStream);
        UploadObjectDto second = new UploadObjectDto(
                "key-2",
                "b.txt", "b.txt", "docs/", "docs/b.txt",
                200L, "text/plain", InputStream::nullInputStream);

        // when
        List<FileRowDto> result = mapper.toFileRows(List.of(first, second));

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(FileRowDto::name)
                .containsExactly("a.txt", "b.txt");
        assertThat(result).extracting(FileRowDto::storageKey)
                .containsExactly("key-1", "key-2");
    }

    @Test
    @DisplayName("Should map directory path, parent path, name")
    void shouldMapPathParentPathAndName() {
        // given
        String path = "docs/reports/";

        // when
        DirectoryRowDto result = mapper.toDirectoryRow(path);

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
        DirectoryRowDto result = mapper.toDirectoryRow(path);

        // then
        assertThat(result.path()).isEqualTo("docs/");
        assertThat(result.parentPath()).isEmpty();
        assertThat(result.name()).isEqualTo("docs/");
    }

    @Test
    @DisplayName("Should map set of directory paths")
    void shouldMapSet() {
        // given
        Set<String> paths = Set.of("docs/", "images/");

        // when
        List<DirectoryRowDto> result = mapper.toDirectoryRows(paths);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(DirectoryRowDto::name)
                .containsExactlyInAnyOrder("docs/", "images/");
    }
}
