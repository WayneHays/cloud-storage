package com.waynehays.cloudfilestorage.service.storage;

import com.waynehays.cloudfilestorage.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.dto.internal.storage.StorageItem;
import com.waynehays.cloudfilestorage.dto.internal.storage.UserPath;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.exception.ResourceStorageOperationException;
import com.waynehays.cloudfilestorage.infrastructure.storage.KeyResolverApi;
import com.waynehays.cloudfilestorage.infrastructure.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.service.storage.ResourceStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceStorageServiceTest {

    @Mock
    private ResourceStorageApi storage;

    @Mock
    private KeyResolverApi keyResolver;

    @InjectMocks
    private ResourceStorageService service;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        when(keyResolver.resolveKey(eq(USER_ID), anyString()))
                .thenAnswer(inv -> "user-1-files/" + inv.getArgument(1));
    }

    @Test
    void putObject_shouldResolveKeyAndStore() {
        // given
        UploadObjectDto object = new UploadObjectDto(
                "file.txt", "file.txt", "docs/", "docs/file.txt",
                100L, "text/plain", InputStream::nullInputStream);

        // when
        service.putObject(USER_ID, object);

        // then
        verify(storage).putObject(any(InputStream.class), eq("user-1-files/docs/file.txt"), eq(100L), eq("text/plain"));
    }

    @Test
    void putObject_shouldThrowOnIOException() {
        // given
        UploadObjectDto object = new UploadObjectDto(
                "file.txt", "file.txt", "docs/", "docs/file.txt",
                100L, "text/plain", () -> { throw new IOException("stream error"); });

        // when & then
        assertThatThrownBy(() -> service.putObject(USER_ID, object))
                .isInstanceOf(ResourceStorageOperationException.class);
    }

    @Test
    void getObject_shouldReturnStorageItem() {
        // given
        StorageItem item = new StorageItem(InputStream.nullInputStream());
        when(storage.getObject("user-1-files/docs/file.txt"))
                .thenReturn(Optional.of(item));

        // when
        StorageItem result = service.getObject(USER_ID, "docs/file.txt");

        // then
        assertThat(result).isSameAs(item);
    }

    @Test
    void getObject_shouldThrowWhenNotFound() {
        // given
        when(storage.getObject("user-1-files/missing.txt"))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.getObject(USER_ID, "missing.txt"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteObject_shouldResolveKeyAndDelete() {
        // when
        service.deleteObject(USER_ID, "docs/file.txt");

        // then
        verify(storage).deleteObject("user-1-files/docs/file.txt");
    }

    @Test
    void deleteObjects_shouldResolveKeysAndDeleteAll() {
        // given
        List<UserPath> userPaths = List.of(
                new UserPath(USER_ID, "a.txt"),
                new UserPath(USER_ID, "b.txt"));

        // when
        service.deleteObjects(userPaths);

        // then
        verify(storage).deleteList(List.of("user-1-files/a.txt", "user-1-files/b.txt"));
    }

    @Test
    void deleteDirectory_shouldResolveKeyAndDeleteByPrefix() {
        // when
        service.deleteDirectory(USER_ID, "docs/");

        // then
        verify(storage).deleteByPrefix("user-1-files/docs/");
    }

    @Test
    void moveObject_shouldResolveBothKeysAndMove() {
        // when
        service.moveObject(USER_ID, "docs/file.txt", "images/file.txt");

        // then
        verify(storage).moveObject("user-1-files/docs/file.txt", "user-1-files/images/file.txt");
    }
}
