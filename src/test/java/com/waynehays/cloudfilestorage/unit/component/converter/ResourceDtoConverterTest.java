package com.waynehays.cloudfilestorage.unit.component.converter;

import com.waynehays.cloudfilestorage.component.converter.ResourceDtoConverterApi;
import com.waynehays.cloudfilestorage.component.converter.ResourceDtoConverter;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.filestorage.dto.MetaData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceDtoConverterTest {

    private final ResourceDtoConverterApi converter = new ResourceDtoConverter();

    @Nested
    class ConvertTests {

        @Test
        @DisplayName("Should convert file from metadata to dto")
        void shouldConvertFileFromMetadataToDto() {
            // given
            MetaData metaData = MetaData.builder()
                    .key("key")
                    .name("file.txt")
                    .size(10L)
                    .contentType("text")
                    .isDirectory(false)
                    .build();
            String path = "docs/file.txt";

            // when
            ResourceDto result = converter.convert(metaData, path);

            // then
            assertThat(result.name()).isEqualTo("file.txt");
            assertThat(result.size()).isEqualTo(10L);
            assertThat(result.type().isFile()).isTrue();
            assertThat(result.type().isDirectory()).isFalse();
            assertThat(result.path()).isEqualTo("docs/");
        }

        @Test
        @DisplayName("Should convert path from metadata to dto")
        void shouldConvertDirectoryFromMetadataToDto() {
            // given
            MetaData metaData = MetaData.builder()
                    .key("key")
                    .name("work")
                    .size(null)
                    .contentType("text")
                    .isDirectory(true)
                    .build();
            String path = "docs/work/";

            // when
            ResourceDto result = converter.convert(metaData, path);

            // then
            assertThat(result.name()).isEqualTo("work");
            assertThat(result.size()).isNull();
            assertThat(result.type().isDirectory()).isTrue();
            assertThat(result.type().isFile()).isFalse();
            assertThat(result.path()).isEqualTo("docs/");
        }

        @Test
        @DisplayName("Should convert file with zero size from metadata to dto")
        void shouldConvertFileWithZeroSizeToDto() {
            // given
            MetaData metaData = MetaData.builder()
                    .size(null)
                    .isDirectory(false)
                    .build();
            String path = "";

            // when
            ResourceDto result = converter.convert(metaData, path);

            // then
            assertThat(result.size()).isNull();
            assertThat(result.type().isFile()).isTrue();
        }
    }

    @Nested
    class FileFromPathTests {

        @Test
        @DisplayName("Should convert file when in root")
        void shouldConvertFile_whenFileInRoot() {
            // when
            ResourceDto result = converter.fileFromPath("/file.txt", 10L);

            // then
            assertThat(result.name()).isEqualTo("file.txt");
            assertThat(result.size()).isEqualTo(10L);
            assertThat(result.type().isFile()).isTrue();
            assertThat(result.path()).isEmpty();
        }

        @Test
        @DisplayName("Should convert file when file in path")
        void shouldConvertFile_whenFileInDirectory() {
            // when
            ResourceDto result = converter.fileFromPath("docs/file.txt", 10L);

            // then
            assertThat(result.name()).isEqualTo("file.txt");
            assertThat(result.type().isFile()).isTrue();
            assertThat(result.size()).isEqualTo(10L);
            assertThat(result.path()).isEqualTo("docs/");
        }

        @Test
        @DisplayName("Should convert file when in deep nested path")
        void shouldConvertFile_whenFileInNestedDirectory() {
            // when
            ResourceDto result = converter.fileFromPath("docs/work/task/file.txt", 10L);

            // then
            assertThat(result.name()).isEqualTo("file.txt");
            assertThat(result.type().isFile()).isTrue();
            assertThat(result.size()).isEqualTo(10L);
            assertThat(result.path()).isEqualTo("docs/work/task/");
        }
    }

    @Nested
    class DirectoryFromPathTests {

        @Test
        @DisplayName("Should convert path when in root")
        void shouldConvertDirectory_whenDirectoryInRoot() {
            // when
            ResourceDto result = converter.directoryFromPath("docs/");

            // then
            assertThat(result.path()).isEmpty();
            assertThat(result.name()).isEqualTo("docs");
            assertThat(result.type().isDirectory()).isTrue();
            assertThat(result.size()).isNull();
        }

        @Test
        @DisplayName("Should convert path when in nested path")
        void shouldConvertDirectory_whenInNestedDirectory() {
            // when
            ResourceDto result = converter.directoryFromPath("docs/work/");

            // then
            assertThat(result.path()).isEqualTo("docs/");
            assertThat(result.name()).isEqualTo("work");
            assertThat(result.type().isDirectory()).isTrue();
            assertThat(result.size()).isNull();
        }

        @Test
        @DisplayName("Should convert path when in deep nested path")
        void shouldConvertDirectory_whenInDeepNestedDirectory() {
            // when
            ResourceDto result = converter.directoryFromPath("docs/work/task/files/");

            // then
            assertThat(result.path()).isEqualTo("docs/work/task/");
            assertThat(result.name()).isEqualTo("files");
            assertThat(result.type().isDirectory()).isTrue();
            assertThat(result.size()).isNull();
        }
    }
}
