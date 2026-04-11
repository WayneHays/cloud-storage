package com.waynehays.cloudfilestorage.unit.service.scheduler;

import com.waynehays.cloudfilestorage.config.properties.StorageQuotaProperties;
import com.waynehays.cloudfilestorage.dto.internal.quota.StorageQuotaDto;
import com.waynehays.cloudfilestorage.dto.internal.quota.UsedSpace;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.quota.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.service.scheduler.quota.StorageQuotaReconciliationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageQuotaReconciliationServiceTest {

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private StorageQuotaProperties properties;

    @Mock
    private StorageQuotaServiceApi quotaService;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private StorageQuotaReconciliationService service;

    @BeforeEach
    void setUp() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    @Nested
    class SuccessfulReconciliation {

        @Test
        void shouldCorrectMismatchedQuotas() {
            // given
            StorageQuotaDto quota = new StorageQuotaDto(1L, 500L, 10000L);
            UsedSpace usedSpace = mock(UsedSpace.class);
            when(usedSpace.getUserId()).thenReturn(1L);
            when(usedSpace.getTotalSize()).thenReturn(300L);

            when(properties.batchSize()).thenReturn(100);
            when(quotaService.findAllQuotas(anyInt(), anyInt()))
                    .thenReturn(new PageImpl<>(List.of(quota)));
            when(metadataService.getUsedSpaceByUsers(List.of(1L)))
                    .thenReturn(List.of(usedSpace));

            // when
            service.reconcileStorageQuotas();

            // then
            verify(quotaService).batchUpdateUsedSpace(argThat(corrections ->
                    corrections.size() == 1
                            && corrections.getFirst().userId().equals(1L)
                            && corrections.getFirst().actualUsedSpace() == 300L
            ));
        }

        @Test
        void shouldSkipUpdateWhenNoMismatches() {
            // given
            StorageQuotaDto quota = new StorageQuotaDto(1L, 500L, 10000L);
            UsedSpace usedSpace = mock(UsedSpace.class);
            when(usedSpace.getUserId()).thenReturn(1L);
            when(usedSpace.getTotalSize()).thenReturn(500L);

            when(properties.batchSize()).thenReturn(100);
            when(quotaService.findAllQuotas(anyInt(), anyInt()))
                    .thenReturn(new PageImpl<>(List.of(quota)));
            when(metadataService.getUsedSpaceByUsers(List.of(1L)))
                    .thenReturn(List.of(usedSpace));

            // when
            service.reconcileStorageQuotas();

            // then
            verify(quotaService, never()).batchUpdateUsedSpace(anyList());
        }

        @Test
        void shouldTreatMissingUsedSpaceAsZero() {
            // given
            StorageQuotaDto quota = new StorageQuotaDto(1L, 500L, 10000L);

            when(properties.batchSize()).thenReturn(100);
            when(quotaService.findAllQuotas(anyInt(), anyInt()))
                    .thenReturn(new PageImpl<>(List.of(quota)));
            when(metadataService.getUsedSpaceByUsers(List.of(1L)))
                    .thenReturn(List.of());

            // when
            service.reconcileStorageQuotas();

            // then
            verify(quotaService).batchUpdateUsedSpace(argThat(corrections ->
                    corrections.size() == 1
                            && corrections.getFirst().actualUsedSpace() == 0L
            ));
        }

        @Test
        void shouldProcessMultiplePages() {
            // given
            StorageQuotaDto quota1 = new StorageQuotaDto(1L, 0L, 10000L);
            StorageQuotaDto quota2 = new StorageQuotaDto(2L, 0L, 10000L);

            Page<StorageQuotaDto> firstPage = new PageImpl<>(
                    List.of(quota1), PageRequest.of(0, 1), 2);
            Page<StorageQuotaDto> secondPage = new PageImpl<>(
                    List.of(quota2), PageRequest.of(1, 1), 2);

            when(properties.batchSize()).thenReturn(1);
            when(quotaService.findAllQuotas(0, 1)).thenReturn(firstPage);
            when(quotaService.findAllQuotas(1, 1)).thenReturn(secondPage);
            when(metadataService.getUsedSpaceByUsers(anyList())).thenReturn(List.of());

            // when
            service.reconcileStorageQuotas();

            // then
            verify(quotaService).findAllQuotas(0, 1);
            verify(quotaService).findAllQuotas(1, 1);
        }
    }

    @Nested
    class ErrorHandling {

        @Test
        void shouldStopOnPageFailure() {
            // given
            when(properties.batchSize()).thenReturn(100);
            when(quotaService.findAllQuotas(anyInt(), anyInt()))
                    .thenThrow(new RuntimeException("DB error"));

            // when
            service.reconcileStorageQuotas();

            // then
            verify(quotaService, never()).batchUpdateUsedSpace(anyList());
        }
    }
}
