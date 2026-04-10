package com.waynehays.cloudfilestorage.unit.mapper;

import com.waynehays.cloudfilestorage.dto.internal.metadata.NewDirectoryDto;
import com.waynehays.cloudfilestorage.dto.internal.metadata.NewFileDto;
import com.waynehays.cloudfilestorage.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.mapper.NewResourceMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NewResourceMapperTest {

    private final NewResourceMapper mapper = Mappers.getMapper(NewResourceMapper.class);

    @Test
    void toNewFile_shouldMapPathParentPathNameAndSize() {
        // given
        UploadObjectDto uploadObject = new UploadObjectDto(
                "report.pdf", "report.pdf", "docs/", "docs/report.pdf",
                2048L, "application/pdf", InputStream::nullInputStream);

        // when
        NewFileDto result = mapper.toNewFile(uploadObject);

        // then
        assertThat(result.path()).isEqualTo("docs/report.pdf");
        assertThat(result.parentPath()).isEqualTo("docs/");
        assertThat(result.name()).isEqualTo("report.pdf");
        assertThat(result.size()).isEqualTo(2048L);
    }

    @Test
    void toNewFile_shouldHandleRootLevelFile() {
        // given
        UploadObjectDto uploadObject = new UploadObjectDto(
                "file.txt", "file.txt", "", "file.txt",
                100L, "text/plain", () -> InputStream.nullInputStream());

        // when
        NewFileDto result = mapper.toNewFile(uploadObject);

        // then
        assertThat(result.path()).isEqualTo("file.txt");
        assertThat(result.parentPath()).isEmpty();
        assertThat(result.name()).isEqualTo("file.txt");
    }

    @Test
    void toNewFiles_shouldMapList() {
        // given
        UploadObjectDto first = new UploadObjectDto(
                "a.txt", "a.txt", "docs/", "docs/a.txt",
                100L, "text/plain", () -> InputStream.nullInputStream());
        UploadObjectDto second = new UploadObjectDto(
                "b.txt", "b.txt", "docs/", "docs/b.txt",
                200L, "text/plain", () -> InputStream.nullInputStream());

        // when
        List<NewFileDto> result = mapper.toNewFiles(List.of(first, second));

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(NewFileDto::name)
                .containsExactly("a.txt", "b.txt");
    }

    @Test
    void toNewDirectory_shouldMapPathParentPathAndName() {
        // given
        String path = "docs/reports/";

        // when
        NewDirectoryDto result = mapper.toNewDirectory(path);

        // then
        assertThat(result.path()).isEqualTo("docs/reports/");
        assertThat(result.parentPath()).isEqualTo("docs/");
        assertThat(result.name()).isEqualTo("reports");
    }

    @Test
    void toNewDirectory_shouldHandleRootLevelDirectory() {
        // given
        String path = "docs/";

        // when
        NewDirectoryDto result = mapper.toNewDirectory(path);

        // then
        assertThat(result.path()).isEqualTo("docs/");
        assertThat(result.parentPath()).isEmpty();
        assertThat(result.name()).isEqualTo("docs");
    }

    @Test
    void toNewDirectories_shouldMapSet() {
        // given
        Set<String> paths = Set.of("docs/", "images/");

        // when
        List<NewDirectoryDto> result = mapper.toNewDirectories(paths);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(NewDirectoryDto::name)
                .containsExactlyInAnyOrder("docs", "images");
    }
}
