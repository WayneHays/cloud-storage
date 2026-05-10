package com.waynehays.cloudfilestorage.core.metadata.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceMetadataTest {

    @Nested
    @DisplayName("Directory constructor")
    class DirectoryConstructor {

        @Test
        @DisplayName("Should create DIRECTORY with all fields set")
        void shouldCreateDirectory() {
            ResourceMetadata directory = new ResourceMetadata(1L, "docs/", "docs/", "", "docs");

            assertThat(directory.getUserId()).isEqualTo(1L);
            assertThat(directory.getPath()).isEqualTo("docs/");
            assertThat(directory.getNormalizedPath()).isEqualTo("docs/");
            assertThat(directory.getParentPath()).isEqualTo("");
            assertThat(directory.getName()).isEqualTo("docs");
            assertThat(directory.getType()).isEqualTo(ResourceType.DIRECTORY);
            assertThat(directory.getStorageKey()).isNull();
            assertThat(directory.getSize()).isNull();
            assertThat(directory.isMarkedForDeletion()).isFalse();
        }

        @Test
        @DisplayName("Should throw NPE when userId is null")
        void shouldThrowWhenUserIdNull() {
            assertThatThrownBy(() -> new ResourceMetadata(null, "docs/", "docs/", "", "docs"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("userId");
        }

        @Test
        @DisplayName("Should throw NPE when path is null")
        void shouldThrowWhenPathNull() {
            assertThatThrownBy(() -> new ResourceMetadata(1L, null, "docs/", "", "docs"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("path");
        }

        @Test
        @DisplayName("Should throw NPE when normalizedPath is null")
        void shouldThrowWhenNormalizedPathNull() {
            assertThatThrownBy(() -> new ResourceMetadata(1L, "docs/", null, "", "docs"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("normalizedPath");
        }

        @Test
        @DisplayName("Should throw NPE when parentPath is null")
        void shouldThrowWhenParentPathNull() {
            assertThatThrownBy(() -> new ResourceMetadata(1L, "docs/", "docs/", null, "docs"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("parentPath");
        }

        @Test
        @DisplayName("Should throw NPE when name is null")
        void shouldThrowWhenNameNull() {
            assertThatThrownBy(() -> new ResourceMetadata(1L, "docs/", "docs/", "", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("name");
        }
    }

    @Nested
    @DisplayName("File constructor")
    class FileConstructor {

        @Test
        @DisplayName("Should create FILE with all fields set")
        void shouldCreateFile() {
            ResourceMetadata file = new ResourceMetadata(
                    1L, "key", "docs/file.txt", "docs/file.txt", "docs/", "file.txt", 100L);

            assertThat(file.getUserId()).isEqualTo(1L);
            assertThat(file.getStorageKey()).isEqualTo("key");
            assertThat(file.getPath()).isEqualTo("docs/file.txt");
            assertThat(file.getNormalizedPath()).isEqualTo("docs/file.txt");
            assertThat(file.getParentPath()).isEqualTo("docs/");
            assertThat(file.getName()).isEqualTo("file.txt");
            assertThat(file.getSize()).isEqualTo(100L);
            assertThat(file.getType()).isEqualTo(ResourceType.FILE);
        }

        @Test
        @DisplayName("Should accept zero size")
        void shouldAcceptZeroSize() {
            ResourceMetadata file = new ResourceMetadata(
                    1L, "key", "empty.txt", "empty.txt", "", "empty.txt", 0L);

            assertThat(file.getSize()).isZero();
        }

        @Test
        @DisplayName("Should throw NPE when storageKey is null")
        void shouldThrowWhenStorageKeyNull() {
            assertThatThrownBy(() -> new ResourceMetadata(
                    1L, null, "f.txt", "f.txt", "", "f.txt", 1L))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("storageKey");
        }

        @Test
        @DisplayName("Should throw NPE when size is null")
        void shouldThrowWhenSizeNull() {
            assertThatThrownBy(() -> new ResourceMetadata(
                    1L, "key", "f.txt", "f.txt", "", "f.txt", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("size");
        }

        @Test
        @DisplayName("Should throw IAE when size is negative")
        void shouldThrowWhenSizeNegative() {
            assertThatThrownBy(() -> new ResourceMetadata(
                    1L, "key", "f.txt", "f.txt", "", "f.txt", -1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("size");
        }
    }

    @Nested
    @DisplayName("Setters")
    class Setters {

        @Test
        @DisplayName("Should allow updating mutable fields")
        void shouldUpdateMutableFields() {
            ResourceMetadata directory = new ResourceMetadata(1L, "old/", "old/", "", "old");

            directory.setPath("new/");
            directory.setNormalizedPath("new/");
            directory.setParentPath("");
            directory.setName("new");
            directory.setMarkedForDeletion(true);

            assertThat(directory.getPath()).isEqualTo("new/");
            assertThat(directory.getNormalizedPath()).isEqualTo("new/");
            assertThat(directory.getParentPath()).isEqualTo("");
            assertThat(directory.getName()).isEqualTo("new");
            assertThat(directory.isMarkedForDeletion()).isTrue();
        }
    }
}