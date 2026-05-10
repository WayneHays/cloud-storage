package com.waynehays.cloudfilestorage.core.metadata.mapper;

import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.core.metadata.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.core.metadata.entity.ResourceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceMetadataMapperTest {

    private final ResourceMetadataMapper mapper = Mappers.getMapper(ResourceMetadataMapper.class);

    @Test
    @DisplayName("Should map all fields")
    void shouldMapAllFields() {
        // given
        ResourceMetadata file = new ResourceMetadata(
                10L, "storage-key-1", "docs/report.pdf",
                "docs/report.pdf", "docs/", "report.pdf", 2048L
        );

        // when
        ResourceMetadataDto result = mapper.toDto(file);

        // then
        assertThat(result.userId()).isEqualTo(10L);
        assertThat(result.storageKey()).isEqualTo("storage-key-1");
        assertThat(result.path()).isEqualTo("docs/report.pdf");
        assertThat(result.parentPath()).isEqualTo("docs/");
        assertThat(result.name()).isEqualTo("report.pdf");
        assertThat(result.size()).isEqualTo(2048L);
        assertThat(result.type()).isEqualTo(ResourceType.FILE);
    }

    @Test
    @DisplayName("Should map directory")
    void shouldMapDirectory() {
        // given
        ResourceMetadata directory = new ResourceMetadata(
                10L, "docs/", "docs/", "", "docs"
        );

        // when
        ResourceMetadataDto result = mapper.toDto(directory);

        // then
        assertThat(result.userId()).isEqualTo(10L);
        assertThat(result.path()).isEqualTo("docs/");
        assertThat(result.name()).isEqualTo("docs");
        assertThat(result.type()).isEqualTo(ResourceType.DIRECTORY);
        assertThat(result.storageKey()).isNull();
        assertThat(result.size()).isNull();
    }

    @Test
    @DisplayName("Should map list")
    void shouldMapList() {
        // given
        ResourceMetadata file = new ResourceMetadata(
                10L, "key-1", "file.txt", "file.txt", "", "file.txt", 100L
        );
        ResourceMetadata directory = new ResourceMetadata(
                10L, "docs/", "docs/", "", "docs"
        );

        // when
        List<ResourceMetadataDto> result = mapper.toDto(List.of(file, directory));

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).type()).isEqualTo(ResourceType.FILE);
        assertThat(result.get(1).type()).isEqualTo(ResourceType.DIRECTORY);
    }
}
