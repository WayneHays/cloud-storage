package com.waynehays.cloudfilestorage.unit.mapper;

import com.waynehays.cloudfilestorage.dto.fileinfo.FileInfoDto;
import com.waynehays.cloudfilestorage.entity.FileInfo;
import com.waynehays.cloudfilestorage.entity.User;
import com.waynehays.cloudfilestorage.mapper.FileInfoMapper;
import com.waynehays.cloudfilestorage.mapper.FileInfoMapperImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FileInfoMapperTest {

    private final FileInfoMapper mapper = new FileInfoMapperImpl();

    @Test
    @DisplayName("Should map FileInfo to FileInfoDto")
    void shouldMapFileInfoToDto() {
        FileInfo fileInfo = FileInfo.builder()
                .id(1L)
                .directory("documents/work")
                .name("report.pdf")
                .storageKey("user-1-files/documents/work/report.pdf")
                .size(1024L)
                .contentType("application/pdf")
                .user(User.builder().id(1L).build())
                .build();

        FileInfoDto result = mapper.toDto(fileInfo);

        assertThat(result).isNotNull();
        assertThat(result.directory()).isEqualTo("documents/work");
        assertThat(result.name()).isEqualTo("report.pdf");
        assertThat(result.storageKey()).isEqualTo("user-1-files/documents/work/report.pdf");
        assertThat(result.size()).isEqualTo(1024L);
        assertThat(result.contentType()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("Should map empty directory")
    void shouldMapEmptyDirectory() {
        FileInfo fileInfo = FileInfo.builder()
                .directory("")
                .name("file.txt")
                .storageKey("user-1-files/file.txt")
                .size(100L)
                .contentType("text/plain")
                .build();

        FileInfoDto result = mapper.toDto(fileInfo);

        assertThat(result.directory()).isEmpty();
    }

    @Test
    @DisplayName("Should map list of FileInfo to list of FileInfoDto")
    void shouldMapList() {
        FileInfo file1 = FileInfo.builder()
                .directory("docs")
                .name("file1.txt")
                .storageKey("key1")
                .size(100L)
                .contentType("text/plain")
                .build();

        FileInfo file2 = FileInfo.builder()
                .directory("docs")
                .name("file2.txt")
                .storageKey("key2")
                .size(200L)
                .contentType("text/plain")
                .build();

        List<FileInfoDto> result = mapper.toDtoList(List.of(file1, file2));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("file1.txt");
        assertThat(result.get(1).name()).isEqualTo("file2.txt");
    }
}
