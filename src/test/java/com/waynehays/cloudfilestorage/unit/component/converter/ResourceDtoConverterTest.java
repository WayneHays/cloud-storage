package com.waynehays.cloudfilestorage.unit.component.converter;

import com.waynehays.cloudfilestorage.component.ResourceDtoConverter;
import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceDtoConverterTest {

    private final ResourceDtoConverter converter = new ResourceDtoConverter();

    @Nested
    class FromMetadata {

        @Test
        void shouldConvertFileMetadata() {
            // given
            ResourceMetadata metadata = new ResourceMetadata();
            metadata.setPath("directory/file.txt");
            metadata.setName("file.txt");
            metadata.setSize(100L);
            metadata.setType(ResourceType.FILE);

            // when
            ResourceDto result = converter.fromMetadata(metadata);

            // then
            assertThat(result.path()).isEqualTo("directory/");
            assertThat(result.name()).isEqualTo("file.txt");
            assertThat(result.size()).isEqualTo(100L);
            assertThat(result.type()).isEqualTo(ResourceType.FILE);
        }

        @Test
        void shouldConvertDirectoryMetadata() {
            // given
            ResourceMetadata metadata = new ResourceMetadata();
            metadata.setPath("directory/subdirectory/");
            metadata.setName("subdirectory");
            metadata.setSize(null);
            metadata.setType(ResourceType.DIRECTORY);

            // when
            ResourceDto result = converter.fromMetadata(metadata);

            // then
            assertThat(result.path()).isEqualTo("directory/");
            assertThat(result.name()).isEqualTo("subdirectory/");
            assertThat(result.size()).isNull();
            assertThat(result.type()).isEqualTo(ResourceType.DIRECTORY);
        }

        @Test
        void shouldConvertRootLevelFile() {
            // given
            ResourceMetadata metadata = new ResourceMetadata();
            metadata.setPath("file.txt");
            metadata.setName("file.txt");
            metadata.setSize(50L);
            metadata.setType(ResourceType.FILE);

            // when
            ResourceDto result = converter.fromMetadata(metadata);

            // then
            assertThat(result.path()).isEmpty();
            assertThat(result.name()).isEqualTo("file.txt");
        }
    }

    @Nested
    class FileFromPath {

        @Test
        void shouldCreateFileDtoFromPath() {
            // given
            String path = "directory/file.txt";
            Long size = 100L;

            // when
            ResourceDto result = converter.fileFromPath(path, size);

            // then
            assertThat(result.path()).isEqualTo("directory/");
            assertThat(result.name()).isEqualTo("file.txt");
            assertThat(result.size()).isEqualTo(100L);
            assertThat(result.type()).isEqualTo(ResourceType.FILE);
        }

        @Test
        void shouldCreateRootLevelFileDtoFromPath() {
            // given
            String path = "file.txt";
            Long size = 50L;

            // when
            ResourceDto result = converter.fileFromPath(path, size);

            // then
            assertThat(result.path()).isEmpty();
            assertThat(result.name()).isEqualTo("file.txt");
            assertThat(result.size()).isEqualTo(50L);
        }
    }

    @Nested
    class DirectoryFromPath {

        @Test
        void shouldCreateDirectoryDtoFromPath() {
            // given
            String path = "directory/subdirectory/";

            // when
            ResourceDto result = converter.directoryFromPath(path);

            // then
            assertThat(result.path()).isEqualTo("directory/");
            assertThat(result.name()).isEqualTo("subdirectory/");
            assertThat(result.size()).isNull();
            assertThat(result.type()).isEqualTo(ResourceType.DIRECTORY);
        }

        @Test
        void shouldCreateRootDirectoryDtoFromPath() {
            // given
            String path = "directory/";

            // when
            ResourceDto result = converter.directoryFromPath(path);

            // then
            assertThat(result.path()).isEmpty();
            assertThat(result.name()).isEqualTo("directory/");
            assertThat(result.size()).isNull();
            assertThat(result.type()).isEqualTo(ResourceType.DIRECTORY);
        }
    }
}
