package com.waynehays.cloudfilestorage.files.api.support;

import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.core.metadata.entity.ResourceType;
import com.waynehays.cloudfilestorage.files.api.dto.response.ResourceResponse;
import com.waynehays.cloudfilestorage.files.operation.upload.dto.UploadObjectDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceResponseMapperTest {

    private final ResourceResponseMapper mapper = Mappers.getMapper(ResourceResponseMapper.class);

    @Nested
    class FromMetadata {

        @Test
        @DisplayName("Should map file metadata with original name")
        void shouldMapFile() {
            // given
            ResourceMetadataDto dto = new ResourceMetadataDto(
                    1L, 10L, "storage-key", "docs/report.pdf", "docs/", "report.pdf",
                    2048L, ResourceType.FILE);

            // when
            ResourceResponse result = mapper.fromResourceMetadataDto(dto);

            // then
            assertThat(result.path()).isEqualTo("docs/");
            assertThat(result.name()).isEqualTo("report.pdf");
            assertThat(result.size()).isEqualTo(2048L);
            assertThat(result.type()).isEqualTo(ResourceType.FILE);
        }

        @Test
        @DisplayName("Should append trailing slash to directory name")
        void shouldAppendSlashToDirectoryName() {
            // given
            ResourceMetadataDto dto = new ResourceMetadataDto(
                    2L, 10L, "storage-key", "docs/", "", "docs",
                    null, ResourceType.DIRECTORY);

            // when
            ResourceResponse result = mapper.fromResourceMetadataDto(dto);

            // then
            assertThat(result.path()).isEqualTo("");
            assertThat(result.name()).isEqualTo("docs/");
            assertThat(result.size()).isNull();
            assertThat(result.type()).isEqualTo(ResourceType.DIRECTORY);
        }

        @Test
        @DisplayName("Should map list of metadata dtos")
        void shouldMapList() {
            // given
            ResourceMetadataDto file = new ResourceMetadataDto(
                    1L, 10L, "key1", "docs/report.pdf", "docs/", "report.pdf", 1024L, ResourceType.FILE);
            ResourceMetadataDto dir = new ResourceMetadataDto(
                    2L, 10L, "key2", "images/", "", "images", null, ResourceType.DIRECTORY);

            // when
            List<ResourceResponse> result = mapper.fromResourceMetadataDto(List.of(file, dir));

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(ResourceResponse::type)
                    .containsExactlyInAnyOrder(ResourceType.FILE, ResourceType.DIRECTORY);
        }
    }

    @Test
    @DisplayName("Should map UploadObjectDto to file response with extracted parent path")
    void fromUploadObjectDto_shouldMapToFileResponse() {
        // given
        UploadObjectDto dto = new UploadObjectDto(
                "storage-key", "report.pdf", "report.pdf",
                "docs/", "docs/report.pdf", 2048L, "application/pdf", null);

        // when
        ResourceResponse result = mapper.fromUploadObjectDto(dto);

        // then
        assertThat(result.path()).isEqualTo("docs/");
        assertThat(result.name()).isEqualTo("report.pdf");
        assertThat(result.size()).isEqualTo(2048L);
        assertThat(result.type()).isEqualTo(ResourceType.FILE);
    }
}