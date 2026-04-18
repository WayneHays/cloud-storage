package com.waynehays.cloudfilestorage.unit.mapper;

import com.waynehays.cloudfilestorage.dto.internal.metadata.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.entity.ResourceType;
import com.waynehays.cloudfilestorage.mapper.ResourceMetadataMapper;
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
        ResourceMetadata metadata = new ResourceMetadata();
        metadata.setId(1L);
        metadata.setUserId(10L);
        metadata.setPath("docs/report.pdf");
        metadata.setParentPath("docs/");
        metadata.setName("report.pdf");
        metadata.setSize(2048L);
        metadata.setType(ResourceType.FILE);

        // when
        ResourceMetadataDto result = mapper.toResourceMetadataDto(metadata);

        // then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.userId()).isEqualTo(10L);
        assertThat(result.path()).isEqualTo("docs/report.pdf");
        assertThat(result.parentPath()).isEqualTo("docs/");
        assertThat(result.name()).isEqualTo("report.pdf");
        assertThat(result.size()).isEqualTo(2048L);
        assertThat(result.type()).isEqualTo(ResourceType.FILE);
    }

    @Test
    @DisplayName("Should map list")
    void shouldMapList() {
        // given
        ResourceMetadata file = new ResourceMetadata();
        file.setId(1L);
        file.setUserId(10L);
        file.setPath("file.txt");
        file.setParentPath("");
        file.setName("file.txt");
        file.setSize(100L);
        file.setType(ResourceType.FILE);

        ResourceMetadata dir = new ResourceMetadata();
        dir.setId(2L);
        dir.setUserId(10L);
        dir.setPath("docs/");
        dir.setParentPath("");
        dir.setName("docs");
        dir.setType(ResourceType.DIRECTORY);

        // when
        List<ResourceMetadataDto> result = mapper.toResourceMetadataDto(List.of(file, dir));

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).path()).isEqualTo("file.txt");
        assertThat(result.get(1).path()).isEqualTo("docs/");
    }

    @Test
    @DisplayName("Should map path and set directory defaults")
    void shouldMapPathAndSetDirectoryDefaults() {
        // given
        Long userId = 10L;
        String path = "docs/reports";

        // when
        ResourceMetadata result = mapper.toDirectoryEntity(userId, path);

        // then
        assertThat(result.getUserId()).isEqualTo(10L);
        assertThat(result.getPath()).isEqualTo("docs/reports/");
        assertThat(result.getParentPath()).isEqualTo("docs/");
        assertThat(result.getName()).isEqualTo("reports");
        assertThat(result.getType()).isEqualTo(ResourceType.DIRECTORY);
        assertThat(result.isMarkedForDeletion()).isFalse();
        assertThat(result.getId()).isNull();
        assertThat(result.getSize()).isNull();
    }

    @Test
    @DisplayName("Should handle trailing slash")
    void shouldHandleTrailingSlash() {
        // given
        Long userId = 10L;
        String path = "docs/reports/";

        // when
        ResourceMetadata result = mapper.toDirectoryEntity(userId, path);

        // then
        assertThat(result.getPath()).isEqualTo("docs/reports/");
        assertThat(result.getParentPath()).isEqualTo("docs/");
        assertThat(result.getName()).isEqualTo("reports");
    }
}
