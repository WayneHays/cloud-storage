package com.waynehays.cloudfilestorage.files.operation;

import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.files.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.core.metadata.ResourceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceDtoMapperTest {

    private final ResourceDtoMapper mapper = Mappers.getMapper(ResourceDtoMapper.class);

    @Test
    @DisplayName("Should map file with original name")
    void shouldMapFileWithOriginalName() {
        // given
        ResourceMetadataDto dto = new ResourceMetadataDto(
                1L, 10L, "docs/report.pdf", "docs/", "report.pdf",
                2048L, ResourceType.FILE);

        // when
        ResourceDto result = mapper.fromResourceMetadataDto(dto);

        // then
        assertThat(result.path()).isEqualTo("docs/");
        assertThat(result.name()).isEqualTo("report.pdf");
        assertThat(result.size()).isEqualTo(2048L);
        assertThat(result.type()).isEqualTo(ResourceType.FILE);
    }

    @Test
    @DisplayName("Should append slash to directory name")
    void shouldAppendSlashToDirectoryName() {
        // given
        ResourceMetadataDto dto = new ResourceMetadataDto(
                2L, 10L, "docs/", "", "docs/",
                null, ResourceType.DIRECTORY);

        // when
        ResourceDto result = mapper.fromResourceMetadataDto(dto);

        // then
        assertThat(result.name()).isEqualTo("docs/");
        assertThat(result.type()).isEqualTo(ResourceType.DIRECTORY);
        assertThat(result.size()).isNull();
    }

    @Test
    @DisplayName("Should extract name and set resource type")
    void shouldExtractNameAndSetResourceType() {
        // given
        String path = "docs/report.pdf";
        Long size = 2048L;

        // when
        ResourceDto result = mapper.fileFromPath(path, size);

        // then
        assertThat(result.path()).isEqualTo("docs/");
        assertThat(result.name()).isEqualTo("report.pdf");
        assertThat(result.size()).isEqualTo(2048L);
        assertThat(result.type()).isEqualTo(ResourceType.FILE);
    }

    @Test
    void directoryFromPath_shouldExtractNameWithSlashAndSetDirectoryType() {
        // given
        String path = "docs/reports/";

        // when
        ResourceDto result = mapper.directoryFromPath(path);

        // then
        assertThat(result.path()).isEqualTo("docs/");
        assertThat(result.name()).isEqualTo("reports/");
        assertThat(result.size()).isNull();
        assertThat(result.type()).isEqualTo(ResourceType.DIRECTORY);
    }

    @Test
    void directoriesFromPaths_shouldMapAllPaths() {
        // given
        Set<String> paths = Set.of("docs/", "images/");

        // when
        List<ResourceDto> result = mapper.directoriesFromPaths(paths);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> r.type() == ResourceType.DIRECTORY);
        assertThat(result).extracting(ResourceDto::name)
                .containsExactlyInAnyOrder("docs/", "images/");
    }
}
