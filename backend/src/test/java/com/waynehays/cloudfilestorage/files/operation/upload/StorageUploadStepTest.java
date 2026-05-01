package com.waynehays.cloudfilestorage.files.operation.upload;

import com.waynehays.cloudfilestorage.files.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.files.operation.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.infrastructure.storage.ResourceStorageException;
import com.waynehays.cloudfilestorage.infrastructure.storage.ResourceStorageServiceApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageUploadStepTest extends BaseUploadStepTest {

    @Mock
    private ResourceStorageServiceApi storageService;

    @Mock
    private ResourceDtoMapper resourceDtoMapper;

    @InjectMocks
    private StorageUploadStep step;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @BeforeEach
    void setUp() {
        step = new StorageUploadStep(storageService, resourceDtoMapper, executor);
    }

    @Test
    @DisplayName("Should upload all files by storageKey and populate result")
    void shouldUploadAllFilesAndPopulateResult() {
        // given
        UploadContext context = uploadContext(
                uploadObject("key-1", "user/1/file1.txt", 100),
                uploadObject("key-2", "user/1/file2.txt", 200)
        );
        ResourceDto dto1 = fileDto("user/1/", "file1.txt", 100);
        ResourceDto dto2 = fileDto("user/1/", "file2.txt", 200);

        when(resourceDtoMapper.fileFromPath("user/1/file1.txt", 100L)).thenReturn(dto1);
        when(resourceDtoMapper.fileFromPath("user/1/file2.txt", 200L)).thenReturn(dto2);

        // when
        step.execute(context);

        // then
        verify(storageService, times(2))
                .putObject(eq(USER_ID), anyString(), anyLong(), anyString(), any());
        assertThat(context.getResult()).containsExactlyInAnyOrder(dto1, dto2);
        assertThat(context.rollbackDto().uploadedStorageKeys())
                .containsExactlyInAnyOrder("key-1", "key-2");
    }

    @Test
    @DisplayName("Should throw when storage operation fails")
    void shouldThrow_whenStorageOperationFails() {
        // given
        UploadContext context = uploadContext(
                uploadObject("key-1", "user/1/file1.txt", 100)
        );
        doThrow(new ResourceStorageException("MinIO unavailable", null))
                .when(storageService).putObject(
                        any(), anyString(), anyLong(), anyString(), any());

        // when & then
        assertThatThrownBy(() -> step.execute(context))
                .isInstanceOf(ResourceStorageException.class);
    }

    @Test
    @DisplayName("Should delete uploaded objects by storageKeys on rollback")
    void shouldDeleteUploadedObjects_whenKeysExist() {
        // given
        UploadRollbackDto snapshot = new UploadRollbackDto(
                USER_ID, 0L, false,
                List.of("key-1", "key-2"),
                List.of()
        );

        // when
        step.rollback(snapshot);

        // then
        verify(storageService).deleteObjects(argThat(map ->
                map.size() == 1
                && map.get(USER_ID).containsAll(List.of("key-1", "key-2"))
        ));
    }

    @Test
    @DisplayName("Should return false when no uploaded keys")
    void shouldReturnFalse_whenNoUploadedKeys() {
        // given
        UploadRollbackDto snapshot = new UploadRollbackDto(
                USER_ID, 0L, false, List.of(), List.of());

        // when & then
        assertThat(step.requiresRollback(snapshot)).isFalse();
    }
}
