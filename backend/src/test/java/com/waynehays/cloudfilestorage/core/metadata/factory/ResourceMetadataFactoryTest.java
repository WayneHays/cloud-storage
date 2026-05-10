package com.waynehays.cloudfilestorage.core.metadata.factory;

import com.waynehays.cloudfilestorage.core.metadata.dto.CreateFileDto;
import com.waynehays.cloudfilestorage.core.metadata.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.core.metadata.entity.ResourceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceMetadataFactoryTest {
    private final ResourceMetadataFactory factory = new ResourceMetadataFactory();

    @Nested
    class CreateDirectory {

        @Test
        @DisplayName("Should set all fields")
        void shouldSetAllFields() {
            // given
            Long userId = 1L;
            String path = "docs/reports/";

            // when
            ResourceMetadata result = factory.createDirectory(userId, path);

            // then
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getPath()).isEqualTo(path);
            assertThat(result.getNormalizedPath()).isEqualTo(path);
            assertThat(result.getParentPath()).isEqualTo("docs/");
            assertThat(result.getName()).isEqualTo("reports");
            assertThat(result.getType()).isEqualTo(ResourceType.DIRECTORY);
            assertThat(result.getStorageKey()).isNull();
            assertThat(result.getSize()).isNull();
        }

        @Test
        @DisplayName("Should set empty parent path for root level directory")
        void shouldSetEmptyParentPath_whenRootLevelDirectory() {
            // given
            String path = "photos";

            // when
            ResourceMetadata result = factory.createDirectory(1L, path);

            // then
            assertThat(result.getParentPath()).isEmpty();
        }

        @Test
        @DisplayName("Should normalize path")
        void shouldNormalizePath() {
            // given
            String path = "Docs/Reports/";

            // when
            ResourceMetadata result = factory.createDirectory(1L, path);

            // then
            assertThat(result.getPath()).isEqualTo(path);
            assertThat(result.getNormalizedPath()).isEqualTo("docs/reports/");
        }

        @Test
        @DisplayName("Should throw when null userId")
        void shouldThrow_whenNullUserId() {
            // when & then
            assertThatThrownBy(() -> factory.createDirectory(null, "docs"))
                    .isInstanceOf(NullPointerException.class);
        }

    }

    @Nested
    class CreateList {

        @Test
        @DisplayName("Should create files correctly")
        void shouldCreateFilesCorrectly() {
            // given
            Long userId = 1L;
            String key1 = "key-1";
            String key2 = "key-2";
            String path1 = "Docs/file1.txt";
            String path2 = "docs/file2.txt";
            List<CreateFileDto> fileData = List.of(
                    new CreateFileDto(key1, path1, 100L),
                    new CreateFileDto(key2, path2, 200L)
            );

            // when
            List<ResourceMetadata> result = factory.createFiles(userId, fileData);

            // then
            assertThat(result).hasSize(2);

            ResourceMetadata first = result.getFirst();
            assertThat(first.getUserId()).isEqualTo(userId);
            assertThat(first.getStorageKey()).isEqualTo(key1);
            assertThat(first.getPath()).isEqualTo(path1);
            assertThat(first.getNormalizedPath()).isEqualTo("docs/file1.txt");
            assertThat(first.getParentPath()).isEqualTo("Docs/");
            assertThat(first.getName()).isEqualTo("file1.txt");
            assertThat(first.getSize()).isEqualTo(100L);
            assertThat(first.getType()).isEqualTo(ResourceType.FILE);
        }

        @Test
        @DisplayName("Should create directories correctly")
        void shouldCreateDirectoriesCorrectly() {
            // given
            Long userId = 1L;
            Set<String> paths = Set.of("docs/", "photos/");

            // when
            List<ResourceMetadata> result = factory.createDirectories(userId, paths);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).allSatisfy(dir -> {
                assertThat(dir.getUserId()).isEqualTo(userId);
                assertThat(dir.getType()).isEqualTo(ResourceType.DIRECTORY);
            });
        }
    }
}
