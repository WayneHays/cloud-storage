package com.waynehays.cloudfilestorage.infrastructure.storage.minio;

import com.waynehays.cloudfilestorage.MinioTestContainer;
import com.waynehays.cloudfilestorage.infrastructure.storage.ResourceStorageException;
import com.waynehays.cloudfilestorage.infrastructure.storage.StorageItem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MinioResourceStorageIntegrationTest {
    private static final String TEST_CONTENT_TYPE = "text/plain";

    private final MinioResourceStorage storage = new MinioResourceStorage(
            MinioTestContainer.getClient(),
            new MinioStorageProperties(
                    MinioTestContainer.BUCKET,
                    100,
                    Duration.ofSeconds(5),
                    Duration.ofSeconds(10),
                    Duration.ofSeconds(10),
                    Duration.ofSeconds(30)
            )
    );

    @AfterEach
    void cleanBucket() {
        MinioTestContainer.cleanTestBucket();
    }

    @AfterAll
    static void tearDown(){
        MinioTestContainer.removeTestBucket();
    }

    private void put(String key, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        storage.putObject(new ByteArrayInputStream(bytes), key, bytes.length, TEST_CONTENT_TYPE);
    }

    private String read(InputStream inputStream) throws Exception {
        try (inputStream) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Nested
    @DisplayName("putObject")
    class PutObject {

        @Test
        @DisplayName("Should store object and make it retrievable")
        void shouldStoreObject() throws Exception {
            // given
            String key = "user-1-files/file.txt";
            String content = "hello world";

            // when
            put(key, content);

            // then
            Optional<StorageItem> result = storage.getObject(key);
            assertThat(result).isPresent();
            assertThat(read(result.get().inputStream())).isEqualTo(content);
        }

        @Test
        @DisplayName("Should overwrite existing object with same key")
        void shouldOverwriteExisting() throws Exception {
            // given
            String key = "user-1-files/file.txt";
            put(key, "first");

            // when
            put(key, "second");

            // then
            Optional<StorageItem> result = storage.getObject(key);
            assertThat(result).isPresent();
            assertThat(read(result.get().inputStream())).isEqualTo("second");
        }
    }

    @Nested
    @DisplayName("getObject")
    class GetObject {

        @Test
        @DisplayName("Should return empty when object not exists")
        void shouldReturnEmpty_WhenMissing() {
            // given
            String missingKey = "user-1-files/missing.txt";

            // when
            Optional<StorageItem> result = storage.getObject(missingKey);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return stream with correct content")
        void shouldReturnContent() throws Exception {
            // given
            String key = "user-1-files/doc.txt";
            put(key, "content");

            // when
            Optional<StorageItem> result = storage.getObject(key);

            // then
            assertThat(result).isPresent();
            assertThat(read(result.get().inputStream())).isEqualTo("content");
        }
    }

    @Nested
    @DisplayName("moveObject")
    class MoveObject {

        @Test
        @DisplayName("Should move object to new key and delete source")
        void shouldMoveObject() throws Exception {
            // given
            String source = "user-1-files/a.txt";
            String target = "user-1-files/b.txt";
            put(source, "content");

            // when
            storage.moveObject(source, target);

            // then
            assertThat(storage.getObject(source)).isEmpty();
            Optional<StorageItem> moved = storage.getObject(target);
            assertThat(moved).isPresent();
            assertThat(read(moved.get().inputStream())).isEqualTo("content");
        }

        @Test
        @DisplayName("Should throw when source not exists")
        void shouldThrow_WhenSourceNotExists() {
            // given
            String source = "user-1-files/missing.txt";
            String target = "user-1-files/target.txt";

            // when & then
            assertThatThrownBy(() -> storage.moveObject(source, target))
                    .isInstanceOf(ResourceStorageException.class);
            assertThat(storage.getObject(target)).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteObject")
    class DeleteObject {

        @Test
        @DisplayName("Should delete existing object")
        void shouldDeleteExisting() {
            // given
            String key = "user-1-files/file.txt";
            put(key, "content");

            // when
            storage.deleteObject(key);

            // then
            assertThat(storage.getObject(key)).isEmpty();
        }

        @Test
        @DisplayName("Should not throw when deleting missing object")
        void shouldNotThrow_WhenMissing() {
            // given
            String missing = "user-1-files/missing.txt";

            // when & then
            storage.deleteObject(missing);
            assertThat(storage.getObject(missing)).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteByPrefix")
    class DeleteByPrefix {

        @Test
        @DisplayName("Should delete all objects by prefix")
        void shouldDeleteAllObjectsByPrefix() {
            // given
            String key1 = "user-1-files/dir/a.txt";
            String key2 = "user-1-files/dir/b.txt";
            String key3 = "user-1-files/dir/sub/c.txt";
            String key4 = "user-1-files/other.txt";
            put(key1, "a");
            put(key2, "b");
            put(key3, "c");
            put(key4, "other");

            // when
            storage.deleteByPrefix("user-1-files/dir/");

            // then
            assertThat(storage.getObject(key1)).isEmpty();
            assertThat(storage.getObject(key2)).isEmpty();
            assertThat(storage.getObject(key3)).isEmpty();
            assertThat(storage.getObject(key4)).isPresent();
        }


        @Test
        @DisplayName("Should do nothing when prefix not exists")
        void shouldDoNothing_WhenPrefixNotExists() {
            // given
            String key = "user-1-files/file.txt";
            put(key, "content");

            // when
            storage.deleteByPrefix("user-1-files/nonexistent/");

            // then
            assertThat(storage.getObject(key)).isPresent();
        }
    }

    @Nested
    @DisplayName("deleteList")
    class DeleteList {

        @Test
        @DisplayName("Should delete all listed objects")
        void shouldDeleteAll() {
            // given
            String key1 = "user-1-files/a.txt";
            String key2 = "user-1-files/b.txt";
            String key3 = "user-1-files/c.txt";
            put(key1, "a");
            put(key2, "b");
            put(key3, "c");

            // when
            storage.deleteList(List.of(key1, key2, key3));

            // then
            assertThat(storage.getObject(key1)).isEmpty();
            assertThat(storage.getObject(key2)).isEmpty();
            assertThat(storage.getObject(key3)).isEmpty();
        }

        @Test
        @DisplayName("Should process more than one batch")
        void shouldProcessMultipleBatches() {
            // given
            List<String> keys = new ArrayList<>();
            for (int i = 0; i <= 2; i++) {
                String key = "user-1-files/batch/f-" + i + ".txt";
                put(key, "x");
                keys.add(key);
            }

            // when
            storage.deleteList(keys);

            // then
            for(String key : keys) {
                assertThat(storage.getObject(key)).isEmpty();
            }
        }

        @Test
        @DisplayName("Should do nothing on empty list")
        void shouldDoNothingOnEmptyList() {
            // given
            String key = "user-1-files/file.txt";
            put(key, "x");

            // when
            storage.deleteList(List.of());

            // then
            assertThat(storage.getObject(key)).isPresent();
        }
    }
}
