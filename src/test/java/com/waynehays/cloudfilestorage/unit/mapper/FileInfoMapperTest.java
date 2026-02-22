package com.waynehays.cloudfilestorage.unit.mapper;

import com.waynehays.cloudfilestorage.dto.files.response.ResourceDto;
import com.waynehays.cloudfilestorage.dto.files.response.ResourceType;
import com.waynehays.cloudfilestorage.entity.FileInfo;
import com.waynehays.cloudfilestorage.entity.User;
import com.waynehays.cloudfilestorage.mapper.FileInfoMapper;
import com.waynehays.cloudfilestorage.mapper.FileInfoMapperImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FileInfoMapperTest {

    private final FileInfoMapper mapper = new FileInfoMapperImpl();

    @Test
    @DisplayName("Should map FileInfo to ResourceDto with FILE type")
    void shouldMapFileInfoToResourceDto() {
        // given
        FileInfo fileInfo = FileInfo.builder()
                .id(1L)
                .directory("documents/work")
                .name("report.pdf")
                .storageKey("123/documents/work/uuid.pdf")
                .size(1024L)
                .contentType("application/pdf")
                .user(User.builder().id(123L).build())
                .build();

        // when
        ResourceDto result = mapper.toResourceDto(fileInfo);

        // then
        assertThat(result).isNotNull();
        assertThat(result.path()).isEqualTo("documents/work");
        assertThat(result.name()).isEqualTo("report.pdf");
        assertThat(result.size()).isEqualTo(1024L);
        assertThat(result.type()).isEqualTo(ResourceType.FILE);
    }

    @Test
    @DisplayName("Should map directory field to path field")
    void shouldMapDirectoryFieldToPathField() {
        // given
        FileInfo fileInfo = FileInfo.builder()
                .directory("documents/work")
                .name("file.txt")
                .size(100L)
                .build();

        // when
        ResourceDto result = mapper.toResourceDto(fileInfo);

        // then
        assertThat(result.path()).isEqualTo("documents/work");
    }

    @Test
    @DisplayName("Should map name field correctly")
    void shouldMapNameFieldCorrectly() {
        // given
        FileInfo fileInfo = FileInfo.builder()
                .directory("docs")
                .name("report.pdf")
                .size(100L)
                .build();

        // when
        ResourceDto result = mapper.toResourceDto(fileInfo);

        // then
        assertThat(result.name()).isEqualTo("report.pdf");
    }

    @Test
    @DisplayName("Should map size field correctly")
    void shouldMapSizeFieldCorrectly() {
        // given
        FileInfo fileInfo = FileInfo.builder()
                .directory("docs")
                .name("file.txt")
                .size(2048L)
                .build();

        // when
        ResourceDto result = mapper.toResourceDto(fileInfo);

        // then
        assertThat(result.size()).isEqualTo(2048L);
    }

    @Test
    @DisplayName("Should set type as FILE")
    void shouldSetTypeAsFile() {
        // given
        FileInfo fileInfo = FileInfo.builder()
                .directory("docs")
                .name("file.txt")
                .size(100L)
                .build();

        // when
        ResourceDto result = mapper.toResourceDto(fileInfo);

        // then
        assertThat(result.type()).isEqualTo(ResourceType.FILE);
    }

    @Test
    @DisplayName("Should map empty directory")
    void shouldMapEmptyDirectory() {
        // given
        FileInfo fileInfo = FileInfo.builder()
                .directory("")
                .name("file.txt")
                .size(100L)
                .build();

        // when
        ResourceDto result = mapper.toResourceDto(fileInfo);

        // then
        assertThat(result.path()).isEmpty();
    }
}
