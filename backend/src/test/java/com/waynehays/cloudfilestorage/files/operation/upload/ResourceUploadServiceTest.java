package com.waynehays.cloudfilestorage.files.operation.upload;

import com.waynehays.cloudfilestorage.files.operation.upload.dto.Context;
import com.waynehays.cloudfilestorage.files.operation.upload.dto.RollbackDto;
import com.waynehays.cloudfilestorage.files.operation.upload.dto.UploadObjectDto;
import com.waynehays.cloudfilestorage.files.operation.upload.step.UploadStep;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceUploadServiceTest {
    private static final Long USER_ID = 1L;

    private UploadObjectDto uploadObject(String storageKey, String fullPath, long size) {
        return new UploadObjectDto(
                storageKey, "file.txt", "file.txt",
                PathUtils.getParentPath(fullPath), fullPath,
                size, "text/plain", InputStream::nullInputStream);
    }

    private ResourceUploadService serviceWith(UploadStep... steps) {
        return new ResourceUploadService(new UploadPipeline(List.of(steps)));
    }

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("Should execute all steps in order")
        void shouldExecuteAllStepsInOrder() {
            // given
            UploadStep step1 = mock(UploadStep.class);
            UploadStep step2 = mock(UploadStep.class);
            UploadStep step3 = mock(UploadStep.class);
            ResourceUploadService service = serviceWith(step1, step2, step3);

            // when
            service.upload(USER_ID, List.of(uploadObject("key-1", "docs/file.txt", 100)));

            // then
            var inOrder = inOrder(step1, step2, step3);
            inOrder.verify(step1).execute(any(Context.class));
            inOrder.verify(step2).execute(any(Context.class));
            inOrder.verify(step3).execute(any(Context.class));
        }
    }

    @Nested
    @DisplayName("rollback")
    class Rollback {

        @Test
        @DisplayName("Should rollback executed steps in reverse order when a step fails")
        void shouldRollbackInReverseOrder_whenStepFails() {
            // given
            UploadStep step1 = mock(UploadStep.class);
            UploadStep step2 = mock(UploadStep.class);
            UploadStep step3 = mock(UploadStep.class);

            when(step1.requiresRollback(any(RollbackDto.class))).thenReturn(true);
            doThrow(new RuntimeException("fail")).when(step2).execute(any());
            when(step2.requiresRollback(any(RollbackDto.class))).thenReturn(true);

            ResourceUploadService service = serviceWith(step1, step2, step3);

            // when & then
            assertThatThrownBy(() -> service.upload(USER_ID, List.of(uploadObject("key-1", "docs/file.txt", 100))))
                    .isInstanceOf(RuntimeException.class);

            var inOrder = inOrder(step2, step1);
            inOrder.verify(step2).rollback(any(RollbackDto.class));
            inOrder.verify(step1).rollback(any(RollbackDto.class));
            verify(step3, never()).execute(any());
            verify(step3, never()).rollback(any());
        }

        @Test
        @DisplayName("Should not rollback a failed step when requiresRollback returns false")
        void shouldSkipRollback_whenRequiresRollbackIsFalse() {
            // given
            UploadStep step1 = mock(UploadStep.class);
            UploadStep step2 = mock(UploadStep.class);

            when(step1.requiresRollback(any(RollbackDto.class))).thenReturn(true);
            doThrow(new RuntimeException("fail")).when(step2).execute(any());
            when(step2.requiresRollback(any(RollbackDto.class))).thenReturn(false);

            ResourceUploadService service = serviceWith(step1, step2);

            // when & then
            assertThatThrownBy(() -> service.upload(USER_ID, List.of(uploadObject("key-1", "docs/file.txt", 100))))
                    .isInstanceOf(RuntimeException.class);

            verify(step1).rollback(any(RollbackDto.class));
            verify(step2, never()).rollback(any());
        }
    }
}