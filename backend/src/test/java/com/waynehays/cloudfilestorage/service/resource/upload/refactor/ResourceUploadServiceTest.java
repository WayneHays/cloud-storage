package com.waynehays.cloudfilestorage.service.resource.upload.refactor;

import com.waynehays.cloudfilestorage.exception.ResourceStorageOperationException;
import com.waynehays.cloudfilestorage.service.resource.upload.CreateDirectoriesStep;
import com.waynehays.cloudfilestorage.service.resource.upload.ReserveQuotaStep;
import com.waynehays.cloudfilestorage.service.resource.upload.ResourceUploadService;
import com.waynehays.cloudfilestorage.service.resource.upload.SaveMetadataStep;
import com.waynehays.cloudfilestorage.service.resource.upload.StorageUploadStep;
import com.waynehays.cloudfilestorage.service.resource.upload.UploadStep;
import com.waynehays.cloudfilestorage.service.resource.upload.ValidateStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ResourceUploadServiceTest {

    @Mock
    private ValidateStep validateStep;
    @Mock
    private ReserveQuotaStep reserveQuotaStep;

    @Mock
    private StorageUploadStep storageUploadStep;

    @Mock
    private SaveMetadataStep saveMetadataStep;

    @Mock
    private CreateDirectoriesStep createDirectoriesStep;

    private ResourceUploadService uploadService;

    @BeforeEach
    void setUp() {
        List<UploadStep> steps = List.of(
                validateStep, reserveQuotaStep, storageUploadStep,
                saveMetadataStep, createDirectoriesStep
        );
        uploadService = new ResourceUploadService(steps);
    }

    @Test
    void upload_shouldExecuteAllStepsInOrder() {
        // given
        InOrder inOrder = inOrder(
                validateStep, reserveQuotaStep, storageUploadStep,
                saveMetadataStep, createDirectoriesStep
        );

        // when
        uploadService.upload(1L, List.of(UploadTestHelper.uploadObject("user/1/file.txt", 100)));

        // then
        inOrder.verify(validateStep).execute(any());
        inOrder.verify(reserveQuotaStep).execute(any());
        inOrder.verify(storageUploadStep).execute(any());
        inOrder.verify(saveMetadataStep).execute(any());
        inOrder.verify(createDirectoriesStep).execute(any());
    }

    @Test
    void upload_shouldRollbackExecutedStepsInReverseOrder_whenStepFails() {
        // given
        doThrow(new ResourceStorageOperationException("Storage down", null))
                .when(storageUploadStep).execute(any());

        InOrder inOrder = inOrder(reserveQuotaStep, validateStep);

        // when & then
        assertThatThrownBy(() -> uploadService.upload(1L, List.of(UploadTestHelper.uploadObject("user/1/file.txt", 100))))
                .isInstanceOf(ResourceStorageOperationException.class);

        inOrder.verify(reserveQuotaStep).rollback(any());
        inOrder.verify(validateStep).rollback(any());
        verify(storageUploadStep, never()).rollback(any());
    }

    @Test
    void upload_shouldContinueRollback_whenOneRollbackStepThrows() {
        // given
        doThrow(new ResourceStorageOperationException("Storage down", null))
                .when(storageUploadStep).execute(any());
        doThrow(new RuntimeException("Quota service unavailable"))
                .when(reserveQuotaStep).rollback(any());

        // when & then
        assertThatThrownBy(() -> uploadService.upload(1L, List.of(UploadTestHelper.uploadObject("user/1/file.txt", 100))))
                .isInstanceOf(ResourceStorageOperationException.class);

        verify(reserveQuotaStep).rollback(any());
        verify(validateStep).rollback(any());
    }
}
