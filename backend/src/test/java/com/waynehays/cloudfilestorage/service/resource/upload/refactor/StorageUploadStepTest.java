package com.waynehays.cloudfilestorage.service.resource.upload.refactor;

import com.waynehays.cloudfilestorage.dto.internal.storage.UserPath;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.exception.ResourceStorageOperationException;
import com.waynehays.cloudfilestorage.mapper.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.service.resource.upload.RollbackSnapshot;
import com.waynehays.cloudfilestorage.service.resource.upload.StorageUploadStep;
import com.waynehays.cloudfilestorage.service.resource.upload.UploadContext;
import com.waynehays.cloudfilestorage.service.storage.ResourceStorageServiceApi;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageUploadStepTest {

    @Mock
    private ResourceStorageServiceApi storageService;

    @Mock
    private ResourceDtoMapper resourceDtoMapper;

    @InjectMocks
    private StorageUploadStep storageUploadStep;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @BeforeEach
    void setUp() {
        storageUploadStep = new StorageUploadStep(storageService, resourceDtoMapper, executor);
    }

    @Test
    void execute_shouldUploadAllFilesAndPopulateResult() {
        // given
        UploadContext context = UploadTestHelper.uploadContext(1L,
                UploadTestHelper.uploadObject("user/1/file1.txt", 100),
                UploadTestHelper.uploadObject("user/1/file2.txt", 200)
        );
        ResourceDto dto1 = UploadTestHelper.fileDto("user/1/", "file1.txt", 100);
        ResourceDto dto2 = UploadTestHelper.fileDto("user/1/", "file2.txt", 200);

        when(resourceDtoMapper.fileFromPath("user/1/file1.txt", 100L)).thenReturn(dto1);
        when(resourceDtoMapper.fileFromPath("user/1/file2.txt", 200L)).thenReturn(dto2);

        // when
        storageUploadStep.execute(context);

        // then
        verify(storageService, times(2)).putObject(eq(1L), any());
        assertThat(context.getResult()).containsExactlyInAnyOrder(dto1, dto2);
        assertThat(context.rollbackSnapshot().uploadedToStoragePaths())
                .containsExactlyInAnyOrder("user/1/file1.txt", "user/1/file2.txt");
    }

    @Test
    void execute_shouldThrow_whenStorageOperationFails() {
        // given
        UploadContext context = UploadTestHelper.uploadContext(1L,
                UploadTestHelper.uploadObject("user/1/file1.txt", 100)
        );
        doThrow(new ResourceStorageOperationException("MinIO unavailable", null))
                .when(storageService).putObject(any(), any());

        // when & then
        assertThatThrownBy(() -> storageUploadStep.execute(context))
                .isInstanceOf(ResourceStorageOperationException.class);
    }

    @Test
    void rollback_shouldDeleteUploadedObjects_whenPathsExist() {
        // given
        RollbackSnapshot snapshot = new RollbackSnapshot(
                1L, 0L, false,
                List.of("user/1/file1.txt", "user/1/file2.txt"),
                List.of()
        );

        // when
        storageUploadStep.rollback(snapshot);

        // then
        verify(storageService).deleteObjects(argThat(paths ->
                paths.size() == 2 &&
                paths.stream().map(UserPath::path).toList()
                        .containsAll(List.of("user/1/file1.txt", "user/1/file2.txt"))
        ));
    }

    @Test
    void rollback_shouldDoNothing_whenNoUploadedPaths() {
        // given
        RollbackSnapshot snapshot = new RollbackSnapshot(1L, 0L, false, List.of(), List.of());

        // when
        storageUploadStep.rollback(snapshot);

        // then
        verifyNoInteractions(storageService);
    }
}
