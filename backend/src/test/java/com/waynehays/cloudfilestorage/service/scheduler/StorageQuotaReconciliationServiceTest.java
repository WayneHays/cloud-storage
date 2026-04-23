package com.waynehays.cloudfilestorage.service.scheduler;

import com.waynehays.cloudfilestorage.config.properties.StorageQuotaProperties;
import com.waynehays.cloudfilestorage.service.quota.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.service.scheduler.quota.StorageQuotaReconciliationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageQuotaReconciliationServiceTest {

    @Mock
    private StorageQuotaProperties properties;

    @Mock
    private StorageQuotaServiceApi quotaService;

    @InjectMocks
    private StorageQuotaReconciliationService service;

    @Nested
    class SuccessfulReconciliation {

        @Test
        @DisplayName("Should reconcile single page of users")
        void shouldReconcileSinglePage() {
            // given
            when(properties.reconciliationBatchSize()).thenReturn(100);
            when(quotaService.findAllUserIds(0, 100))
                    .thenReturn(new PageImpl<>(List.of(1L, 2L)));

            // when
            service.reconcileStorageQuotas();

            // then
            verify(quotaService).reconcileUsedSpace(List.of(1L, 2L));
        }

        @Test
        @DisplayName("Should process multiple pages until last page")
        void shouldProcessMultiplePages() {
            // given
            Page<Long> firstPage = new PageImpl<>(
                    List.of(1L), PageRequest.of(0, 1), 2);
            Page<Long> secondPage = new PageImpl<>(
                    List.of(2L), PageRequest.of(1, 1), 2);

            when(properties.reconciliationBatchSize()).thenReturn(1);
            when(quotaService.findAllUserIds(0, 1)).thenReturn(firstPage);
            when(quotaService.findAllUserIds(1, 1)).thenReturn(secondPage);

            // when
            service.reconcileStorageQuotas();

            // then
            verify(quotaService).reconcileUsedSpace(List.of(1L));
            verify(quotaService).reconcileUsedSpace(List.of(2L));
        }

        @Test
        @DisplayName("Should do nothing when no quotas exist")
        void shouldDoNothingWhenNoQuotas() {
            // given
            when(properties.reconciliationBatchSize()).thenReturn(100);
            when(quotaService.findAllUserIds(0, 100))
                    .thenReturn(Page.empty());

            // when
            service.reconcileStorageQuotas();

            // then
            verify(quotaService, never()).reconcileUsedSpace(anyList());
        }
    }

    @Nested
    class ErrorHandling {

        @Test
        @DisplayName("Should stop on page failure without processing further")
        void shouldStopOnPageFailure() {
            // given
            when(properties.reconciliationBatchSize()).thenReturn(100);
            when(quotaService.findAllUserIds(anyInt(), anyInt()))
                    .thenThrow(new RuntimeException("DB error"));

            // when
            service.reconcileStorageQuotas();

            // then
            verify(quotaService, never()).reconcileUsedSpace(anyList());
        }

        @Test
        @DisplayName("Should stop on reconcile failure without processing next page")
        void shouldStopOnReconcileFailure() {
            // given
            Page<Long> firstPage = new PageImpl<>(
                    List.of(1L), PageRequest.of(0, 1), 2);

            when(properties.reconciliationBatchSize()).thenReturn(1);
            when(quotaService.findAllUserIds(0, 1)).thenReturn(firstPage);
            doThrow(new RuntimeException("DB error"))
                    .when(quotaService).reconcileUsedSpace(List.of(1L));

            // when
            service.reconcileStorageQuotas();

            // then
            verify(quotaService, never()).findAllUserIds(eq(1), anyInt());
        }
    }
}
