package com.waynehays.cloudfilestorage.unit.component.converter;

import com.waynehays.cloudfilestorage.filestorage.dto.MetaData;
import com.waynehays.cloudfilestorage.filestorage.minio.MinioConverter;
import io.minio.StatObjectResponse;
import io.minio.messages.Item;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinioConverterTest {

    private final MinioConverter converter = new MinioConverter();

    @Nested
    class FileFromItemTests {

        @Test
        void toMetaData_NormalFile_ShouldReturnCorrectMetaData() {
            // given
            Item mockItem = mock(Item.class);
            String expectedKey = "docs/file.txt";
            String expectedName = "file.txt";
            long expectedSize = 10L;

            when(mockItem.objectName()).thenReturn(expectedKey);
            when(mockItem.size()).thenReturn(expectedSize);
            when(mockItem.isDir()).thenReturn(false);

            // when
            MetaData result = converter.toMetaData(mockItem);

            // then
            assertThat(result).isNotNull();
            assertThat(result.key()).isEqualTo(expectedKey);
            assertThat(result.name()).isEqualTo(expectedName);
            assertThat(result.size()).isEqualTo(expectedSize);
            assertThat(result.isDirectory()).isFalse();
        }

        @Test
        void toMetaData_FileInRoot_ShouldReturnCorrectMetaData() {
            // given
            Item mockItem = mock(Item.class);
            String expectedKey = "file.txt";
            String expectedName = "file.txt";

            when(mockItem.objectName()).thenReturn(expectedKey);

            // when
            MetaData result = converter.toMetaData(mockItem);

            // then
            assertThat(result).isNotNull();
            assertThat(result.key()).isEqualTo(expectedKey);
            assertThat(result.name()).isEqualTo(expectedName);
            assertThat(result.isDirectory()).isFalse();
        }

        @Test
        void toMetaData_DeepNestedFile_ShouldReturnCorrectMetaData() {
            // given
            Item mockItem = mock(Item.class);
            String expectedKey = "docs/files/work/test/file.txt";
            String expectedName = "file.txt";
            long size = 128L;

            when(mockItem.objectName()).thenReturn(expectedKey);
            when(mockItem.size()).thenReturn(size);

            // when
            MetaData result = converter.toMetaData(mockItem);

            // then
            assertThat(result).isNotNull();
            assertThat(result.key()).isEqualTo(expectedKey);
            assertThat(result.name()).isEqualTo(expectedName);
            assertThat(result.size()).isEqualTo(size);
            assertThat(result.isDirectory()).isFalse();
        }

        @Test
        void toMetaData_FileWithoutExtension_ShouldReturnCorrectMetaData() {
            // given
            Item mockItem = mock(Item.class);
            String expectedKey = "docs/files/work/test/file";
            String expectedName = "file";

            when(mockItem.objectName()).thenReturn(expectedKey);

            // when
            MetaData result = converter.toMetaData(mockItem);

            // then
            assertThat(result).isNotNull();
            assertThat(result.key()).isEqualTo(expectedKey);
            assertThat(result.name()).isEqualTo(expectedName);
            assertThat(result.isDirectory()).isFalse();
        }
    }

    @Nested
    class DirectoryFromItemTests {

        @Test
        void toMetaData_DirectoryIsDirTrue_ShouldReturnCorrectMetaData() {
            // given
            Item mockItem = mock(Item.class);
            String expectedKey = "docs/";
            String expectedName = "docs";

            when(mockItem.objectName()).thenReturn(expectedKey);
            when(mockItem.isDir()).thenReturn(true);

            // when
            MetaData result = converter.toMetaData(mockItem);

            // then
            assertThat(result).isNotNull();
            assertThat(result.key()).isEqualTo(expectedKey);
            assertThat(result.name()).isEqualTo(expectedName);
            assertThat(result.size()).isNull();
            assertThat(result.isDirectory()).isTrue();
        }

        @Test
        void toMetaData_DirectoryWithTrailingSlash_ShouldReturnCorrectMetaData() {
            // given
            Item mockItem = mock(Item.class);
            String expectedKey = "docs/files/";
            String expectedName = "files";

            when(mockItem.objectName()).thenReturn(expectedKey);
            when(mockItem.isDir()).thenReturn(false);

            // when
            MetaData result = converter.toMetaData(mockItem);

            // then
            assertThat(result).isNotNull();
            assertThat(result.key()).isEqualTo(expectedKey);
            assertThat(result.name()).isEqualTo(expectedName);
            assertThat(result.size()).isNull();
            assertThat(result.isDirectory()).isTrue();
        }

        @Test
        void toMetaData_DirectoryDeepNested_ShouldReturnCorrectMetaData() {
            // given
            Item mockItem = mock(Item.class);
            String expectedKey = "docs/files/work/task/";
            String expectedName = "task";

            when(mockItem.isDir()).thenReturn(true);
            when(mockItem.objectName()).thenReturn(expectedKey);

            // when
            MetaData result = converter.toMetaData(mockItem);

            // then
            assertThat(result).isNotNull();
            assertThat(result.key()).isEqualTo(expectedKey);
            assertThat(result.name()).isEqualTo(expectedName);
            assertThat(result.size()).isNull();
            assertThat(result.isDirectory()).isTrue();
        }
    }

    @Nested
    class FromStatObjectResponseTests {

        @Test
        void toMetaData_NormalFile_ShouldReturnCorrectMetaData() {
            // given
            StatObjectResponse mockResponse = mock(StatObjectResponse.class);
            String expectedKey = "docs/file.txt";
            String expectedName = "file.txt";

            when(mockResponse.object()).thenReturn(expectedKey);
            when(mockResponse.size()).thenReturn(128L);
            when(mockResponse.contentType()).thenReturn("text");

            // when
            MetaData result = converter.toMetaData(mockResponse);

            // then
            assertThat(result.name()).isEqualTo(expectedName);
            assertThat(result.contentType()).isEqualTo("text");
            assertThat(result.isDirectory()).isFalse();
            assertThat(result.size()).isEqualTo(128L);
            assertThat(result.key()).isEqualTo(expectedKey);
        }

        @Test
        void toMetaData_FileWithoutExtension_ShouldReturnCorrectMetaData() {
            // given
            StatObjectResponse mockResponse = mock(StatObjectResponse.class);
            String expectedKey = "docs/file";
            String expectedName = "file";

            when(mockResponse.object()).thenReturn(expectedKey);

            // when
            MetaData result = converter.toMetaData(mockResponse);

            // then
            assertThat(result.name()).isEqualTo(expectedName);
            assertThat(result.isDirectory()).isFalse();
            assertThat(result.key()).isEqualTo(expectedKey);
        }

        @Test
        void toMetaData_DirectoryWithTrailingSlash_ShouldReturnCorrectMetaData() {
            // given
            StatObjectResponse mockResponse = mock(StatObjectResponse.class);
            String expectedKey = "docs/files/";
            String expectedName = "files";

            when(mockResponse.object()).thenReturn(expectedKey);

            // when
            MetaData result = converter.toMetaData(mockResponse);

            // then
            assertThat(result.name()).isEqualTo(expectedName);
            assertThat(result.isDirectory()).isTrue();
            assertThat(result.size()).isNull();
            assertThat(result.key()).isEqualTo(expectedKey);
        }

        @Test
        void toMetaData_NestedDirectory_ShouldReturnCorrectMetaData() {
            StatObjectResponse mockResponse = mock(StatObjectResponse.class);
            String expectedKey = "docs/files/task/work/";
            String expectedName = "work";

            when(mockResponse.object()).thenReturn(expectedKey);

            // when
            MetaData result = converter.toMetaData(mockResponse);

            // then
            assertThat(result.name()).isEqualTo(expectedName);
            assertThat(result.isDirectory()).isTrue();
            assertThat(result.size()).isNull();
            assertThat(result.key()).isEqualTo(expectedKey);
        }
    }
}
