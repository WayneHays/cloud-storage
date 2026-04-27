package com.waynehays.cloudfilestorage.files.operation.upload;

import com.waynehays.cloudfilestorage.core.quota.StorageQuotaServiceApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ReserveQuotaStepTest {

    @Mock
    private StorageQuotaServiceApi quotaService;

    @InjectMocks
    private ReserveQuotaStep reserveQuotaStep;

    @Test
    void execute_shouldReserveSpaceAndMarkQuotaReserved() {
        // given
        UploadContext context = UploadTestHelper.uploadContext(1L,
                UploadTestHelper.uploadObject("user/1/file1.txt", 300),
                UploadTestHelper.uploadObject("user/1/file2.txt", 700)
        );

        // when
        reserveQuotaStep.execute(context);

        // then
        verify(quotaService).reserveSpace(1L, 1000L);
        assertThat(context.rollbackSnapshot().quotaReserved()).isTrue();
    }

    @Test
    void rollback_shouldReleaseSpace_whenQuotaWasReserved() {
        // given
        UploadRollbackDto snapshot = new UploadRollbackDto(1L, 1000L, true, List.of(), List.of());

        // when
        reserveQuotaStep.rollback(snapshot);

        // then
        verify(quotaService).releaseSpace(1L, 1000L);
    }

    @Test
    void rollback_shouldDoNothing_whenQuotaWasNotReserved() {
        // given
        UploadRollbackDto snapshot = new UploadRollbackDto(1L, 1000L, false, List.of(), List.of());

        // when
        reserveQuotaStep.rollback(snapshot);

        // then
        verifyNoInteractions(quotaService);
    }
}
