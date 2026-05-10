package com.waynehays.cloudfilestorage.files.operation.upload.step;

import com.waynehays.cloudfilestorage.files.api.dto.response.ResourceResponse;
import com.waynehays.cloudfilestorage.files.api.support.ResourceResponseMapper;
import com.waynehays.cloudfilestorage.files.operation.upload.dto.Context;
import com.waynehays.cloudfilestorage.files.operation.upload.dto.RollbackDto;
import com.waynehays.cloudfilestorage.infrastructure.storage.ResourceStorageServiceApi;
import com.waynehays.cloudfilestorage.infrastructure.storage.exception.ResourceStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private ResourceResponseMapper resourceResponseMapper;

    private StorageUploadStep step;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @BeforeEach
    void setUp() {
        step = new StorageUploadStep(executor, storageService, resourceResponseMapper);
    }

    @Test
    @DisplayName("Should upload all files by storageKey and populate result")
    void shouldUploadAllFilesAndPopulateResult() {
        // given
        Context context = uploadContext(
                uploadObject("key-1", "user/1/file1.txt", 100),
                uploadObject("key-2", "user/1/file2.txt", 200)
        );
        ResourceResponse dto1 = fileDto("user/1/", "file1.txt", 100);
        ResourceResponse dto2 = fileDto("user/1/", "file2.txt", 200);

        when(resourceResponseMapper.fromUploadObjectDto(argThat(o -> o != null && "key-1".equals(o.storageKey())))).thenReturn(dto1);
        when(resourceResponseMapper.fromUploadObjectDto(argThat(o -> o != null && "key-2".equals(o.storageKey())))).thenReturn(dto2);

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
        Context context = uploadContext(
                uploadObject("key-1", "user/1/file1.txt", 100)
        );
        doThrow(new ResourceStorageException("MinIO unavailable", null))
                .when(storageService).putObject(any(), anyString(), anyLong(), anyString(), any());

        // when & then
        assertThatThrownBy(() -> step.execute(context))
                .isInstanceOf(ResourceStorageException.class);
    }

    @Test
    @DisplayName("Should delete uploaded objects by storageKeys on rollback")
    void shouldDeleteUploadedObjects_whenKeysExist() {
        // given
        RollbackDto snapshot = new RollbackDto(
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
    @DisplayName("Should return false from requiresRollback when no uploaded keys")
    void shouldReturnFalse_whenNoUploadedKeys() {
        // given
        RollbackDto snapshot = new RollbackDto(
                USER_ID, 0L, false, List.of(), List.of());

        // when & then
        assertThat(step.requiresRollback(snapshot)).isFalse();
    }
}