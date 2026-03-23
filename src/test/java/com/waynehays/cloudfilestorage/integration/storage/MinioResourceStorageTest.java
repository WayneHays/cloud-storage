package com.waynehays.cloudfilestorage.integration.storage;

import com.waynehays.cloudfilestorage.exception.FileStorageException;
import com.waynehays.cloudfilestorage.integration.base.AbstractIntegrationBaseTest;
import com.waynehays.cloudfilestorage.integration.base.MinioTestCleaner;
import com.waynehays.cloudfilestorage.storage.dto.StorageItem;
import com.waynehays.cloudfilestorage.storage.minio.MinioResourceStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class MinioResourceStorageTest extends AbstractIntegrationBaseTest {

    @Autowired
    private MinioResourceStorage storage;

    @Autowired
    private MinioTestCleaner cleaner;

    @AfterEach
    void clean() {
        cleaner.deleteAll();
    }

    @Nested
    class PutAndGetObject {

        @Test
        void shouldUploadAndDownloadFile() throws IOException {
            // given
            String key = "user-1-files/directory/file.txt";
            byte[] content = "test-content".getBytes();
            InputStream inputStream = new ByteArrayInputStream(content);

            // when
            storage.putObject(inputStream, key, content.length, "text/plain");

            // then
            Optional<StorageItem> result = storage.getObject(key);
            assertThat(result).isPresent();
            try (InputStream is = result.get().inputStream()) {
                assertThat(is.readAllBytes()).isEqualTo(content);
            }
        }

        @Test
        void shouldReturnEmpty_whenObjectNotFound() {
            // given
            String key = "user-1-files/nonexistent.txt";

            // when
            Optional<StorageItem> result = storage.getObject(key);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class CreateDirectory {

        @Test
        void shouldCreateDirectoryMarker() {
            // given
            String key = "user-1-files/directory/";

            // when
            storage.createDirectory(key);

            // then
            Optional<StorageItem> result = storage.getObject(key);
            assertThat(result).isPresent();
        }
    }

    @Nested
    class Delete {

        @Test
        void shouldDeleteObject() {
            // given
            String key = "user-1-files/directory/file.txt";
            storage.putObject(new ByteArrayInputStream("content".getBytes()), key, 7, "text/plain");

            // when
            storage.delete(key);

            // then
            Optional<StorageItem> result = storage.getObject(key);
            assertThat(result).isEmpty();
        }

        @Test
        void shouldNotThrowException_whenDeleteNonExistent() {
            // given
            String key = "user-1-files/nonexistent.txt";

            // when & then
            assertThatCode(() -> storage.delete(key))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class DeleteByPrefix {

        @Test
        void shouldDeleteAllObjectsByPrefix() {
            // given
            String prefix = "user-1-files/directory/";
            storage.putObject(new ByteArrayInputStream("content1".getBytes()),
                    prefix + "file1.txt", 8, "text/plain");
            storage.putObject(new ByteArrayInputStream("content2".getBytes()),
                    prefix + "file2.txt", 8, "text/plain");
            storage.putObject(new ByteArrayInputStream("content3".getBytes()),
                    prefix + "sub/file3.txt", 8, "text/plain");

            // when
            storage.deleteByPrefix(prefix);

            // then
            assertThat(storage.getObject(prefix + "file1.txt")).isEmpty();
            assertThat(storage.getObject(prefix + "file2.txt")).isEmpty();
            assertThat(storage.getObject(prefix + "sub/file3.txt")).isEmpty();
        }

        @Test
        void shouldNotDeleteObjectsWithDifferentPrefix() {
            // given
            storage.putObject(new ByteArrayInputStream("content1".getBytes()),
                    "user-1-files/directory/file.txt", 8, "text/plain");
            storage.putObject(new ByteArrayInputStream("content2".getBytes()),
                    "user-1-files/other/file.txt", 8, "text/plain");

            // when
            storage.deleteByPrefix("user-1-files/directory/");

            // then
            assertThat(storage.getObject("user-1-files/other/file.txt")).isPresent();
        }
    }

    @Nested
    class Move {

        @Test
        void shouldMoveObject() throws IOException {
            // given
            String sourceKey = "user-1-files/directory/file.txt";
            String targetKey = "user-1-files/other/file.txt";
            byte[] content = "content".getBytes();
            storage.putObject(new ByteArrayInputStream(content), sourceKey, content.length, "text/plain");

            // when
            storage.move(sourceKey, targetKey);

            // then
            assertThat(storage.getObject(sourceKey)).isEmpty();

            Optional<StorageItem> moved = storage.getObject(targetKey);
            assertThat(moved).isPresent();
            try (InputStream is = moved.get().inputStream()) {
                assertThat(is.readAllBytes()).isEqualTo(content);
            }
        }

        @Test
        void shouldThrowWhenMovingNonexistentObject() {
            // given
            String sourceKey = "user-1-files/nonexistent.txt";
            String targetKey = "user-1-files/other/file.txt";

            // when & then
            assertThatThrownBy(() -> storage.move(sourceKey, targetKey))
                    .isInstanceOf(FileStorageException.class);
        }
    }
}
