package com.waynehays.cloudfilestorage.files.operation.upload.step;

import com.waynehays.cloudfilestorage.core.quota.service.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.files.operation.upload.dto.Context;
import com.waynehays.cloudfilestorage.files.operation.upload.dto.RollbackDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReserveQuotaStepTest extends BaseUploadStepTest {

    @Mock
    private StorageQuotaServiceApi quotaService;

    @InjectMocks
    private ReserveQuotaStep reserveQuotaStep;

    @Test
    void execute_shouldReserveSpaceAndMarkQuotaReserved() {
        // given
        Context context = uploadContext(
                uploadObject("key-1", "user/1/file1.txt", 300),
                uploadObject("key-2", "user/1/file2.txt", 700)
        );

        // when
        reserveQuotaStep.execute(context);

        // then
        verify(quotaService).reserveSpace(USER_ID, 1000L);
        assertThat(context.rollbackDto().quotaReserved()).isTrue();
    }

    @Test
    void rollback_shouldReleaseSpace_whenQuotaWasReserved() {
        // given
        RollbackDto snapshot = new RollbackDto(USER_ID, 1000L, true, List.of(), List.of());

        // when
        reserveQuotaStep.rollback(snapshot);

        // then
        verify(quotaService).releaseSpace(USER_ID, 1000L);
    }
}