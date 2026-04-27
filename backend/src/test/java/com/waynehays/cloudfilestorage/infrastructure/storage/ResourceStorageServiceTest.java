package com.waynehays.cloudfilestorage.infrastructure.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceStorageServiceTest {

    @Mock
    private ResourceStorageApi storage;

    @InjectMocks
    private ResourceStorageService service;

    private static final Long USER_ID = 1L;

    @Nested
    class PutObject {

        @Test
        @DisplayName("should resolve key and store object")
        void shouldResolveKeyAndStoreObject() {
            // given
            String fullPath = "docs/file.txt";
            long size = 100L;
            String contentType = "text/plain";
            InputStreamSupplier inputStreamSupplier = InputStream::nullInputStream;

            // when
            service.putObject(USER_ID, fullPath, size, contentType, inputStreamSupplier);

            // then
            verify(storage).putObject(
                    any(InputStream.class),
                    eq("user-1-files/docs/file.txt"),
                    eq(100L),
                    eq(contentType));
        }

        @Test
        @DisplayName("should throw when IOException on inputstream open")
        void shouldThrow_whenIoException() {
            // when & then
            assertThatThrownBy(() -> service.putObject(
                    USER_ID,
                    "docs/file.txt",
                    100L,
                    "text/plain",
                    () -> {
                        throw new IOException("stream error");
                    }))
                    .isInstanceOf(ResourceStorageOperationException.class);
        }
    }

    @Test
    @DisplayName("getObject() should return optional of StorageItem")
    void shouldReturnOptionalOfStorageItem() {
        // given
        Optional<StorageItem> expected = Optional.of(new StorageItem(InputStream.nullInputStream()));
        when(storage.getObject("user-1-files/docs/file.txt"))
                .thenReturn(expected);

        // when
        Optional<StorageItem> result = service.getObject(USER_ID, "docs/file.txt");

        // then
        assertThat(result).isPresent().isEqualTo(expected);
    }

    @Test
    @DisplayName("deleteObject() should resolve key and delete object")
    void shouldResolveKeyAndDelete() {
        // when
        service.deleteObject(USER_ID, "docs/file.txt");

        // then
        verify(storage).deleteObject("user-1-files/docs/file.txt");
    }

    @Test
    @DisplayName("deleteObjects() should resolve keys from all users and delete all")
    void deleteObjects_shouldResolveKeysAndDeleteAll() {
        // given
        Map<Long, List<String>> pathsByUser = Map.of(
                1L, List.of("a.txt", "b.txt"),
                2L, List.of("c.txt")
        );

        // when
        service.deleteObjects(pathsByUser);

        // then
        verify(storage).deleteList(argThat(keys ->
                keys.size() == 3
                && keys.contains("user-1-files/a.txt")
                && keys.contains("user-1-files/b.txt")
                && keys.contains("user-2-files/c.txt")
        ));
    }

    @Test
    @DisplayName("deleteDirectory() should resolve key and delete by prefix")
    void deleteDirectory_shouldResolveKeyAndDeleteByPrefix() {
        // when
        service.deleteDirectory(USER_ID, "docs/");

        // then
        verify(storage).deleteByPrefix("user-1-files/docs/");
    }

    @Test
    @DisplayName("moveObject() should resolve key from and key to and move")
    void moveObject_shouldResolveBothKeysAndMove() {
        // when
        service.moveObject(USER_ID, "docs/file.txt", "images/file.txt");

        // then
        verify(storage).moveObject("user-1-files/docs/file.txt", "user-1-files/images/file.txt");
    }
}
