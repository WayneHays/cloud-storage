package com.waynehays.cloudfilestorage.quota.service;

import com.waynehays.cloudfilestorage.quota.entity.StorageQuota;
import com.waynehays.cloudfilestorage.quota.repository.StorageQuotaRepository;
import com.waynehays.cloudfilestorage.shared.exception.ResourceStorageLimitException;
import com.waynehays.cloudfilestorage.shared.exception.StorageQuotaNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageQuotaServiceTest {

    @Mock
    private StorageQuotaRepository repository;

    @InjectMocks
    private StorageQuotaService service;

    private static final Long USER_ID = 1L;

    @Nested
    @DisplayName("findAllUserIds")
    class FindAllUserIds {

        @Test
        @DisplayName("Should find all user ids")
        void shouldFindAllUserIds() {
            // given
            int page = 0;
            int limit = 5;

            List<Long> userIds = List.of(1L, 2L, 3L, 4L, 5L);
            Page<Long> expectedPage = new PageImpl<>(userIds,
                    PageRequest.of(page, limit), userIds.size());
            when(repository.findAllUserIds(PageRequest.of(page, limit)))
                    .thenReturn(expectedPage);

            // when
            Page<Long> result = service.findAllUserIds(page, limit);

            // then
            assertEquals(expectedPage, result);
            assertEquals(userIds.size(), result.getTotalElements());
            assertEquals(1, result.getTotalPages());
            assertEquals(userIds, result.getContent());
            verify(repository).findAllUserIds(PageRequest.of(page, limit));
        }

        @Test
        void testFindAllUserIds_EmptyPage() {
            int page = 1;
            int limit = 10;
            Page<Long> emptyPage = new PageImpl<>(List.of(), PageRequest.of(page, limit), 0);

            when(repository.findAllUserIds(PageRequest.of(page, limit)))
                    .thenReturn(emptyPage);

            Page<Long> result = service.findAllUserIds(page, limit);

            assertEquals(0, result.getTotalElements());
            assertTrue(result.isEmpty());
        }

        @Test
        void testFindAllUserIds_DifferentPageAndLimit() {
            int page = 2;
            int limit = 5;
            List<Long> userIds = List.of(11L, 12L, 13L);
            Page<Long> expectedPage = new PageImpl<>(userIds, PageRequest.of(page, limit), 15);

            when(repository.findAllUserIds(PageRequest.of(page, limit)))
                    .thenReturn(expectedPage);

            Page<Long> result = service.findAllUserIds(page, limit);

            assertEquals(3, result.getNumberOfElements());
            assertEquals(15, result.getTotalElements());
        }
    }

    @Nested
    @DisplayName("createAndSaveQuota")
    class CreateAndSaveQuota {

        @Test
        void shouldCreateAndSaveQuota() {
            // given
            ArgumentCaptor<StorageQuota> captor = ArgumentCaptor.forClass(StorageQuota.class);

            // when
            service.createStorageQuota(USER_ID, 10000L);

            // then
            verify(repository).saveAndFlush(captor.capture());
            StorageQuota saved = captor.getValue();
            assertThat(saved.getUsedSpace()).isEqualTo(0);
            assertThat(saved.getStorageLimit()).isEqualTo(10000L);
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
        }
    }

    @Nested
    class ReserveSpace {

        @Test
        void shouldReserveSpaceWhenEnoughFree() {
            // given
            StorageQuota quota = new StorageQuota();
            quota.setUsedSpace(200L);
            quota.setStorageLimit(1000L);

            when(repository.findByUserIdWithLock(USER_ID))
                    .thenReturn(Optional.of(quota));

            // when
            service.reserveSpace(USER_ID, 300L);

            // then
            assertThat(quota.getUsedSpace()).isEqualTo(500L);
        }

        @Test
        void shouldThrowWhenNotEnoughSpace() {
            // given
            StorageQuota quota = new StorageQuota();
            quota.setUsedSpace(900L);
            quota.setStorageLimit(1000L);

            when(repository.findByUserIdWithLock(USER_ID))
                    .thenReturn(Optional.of(quota));

            // when & then
            assertThatThrownBy(() -> service.reserveSpace(USER_ID, 200L))
                    .isInstanceOf(ResourceStorageLimitException.class);
            assertThat(quota.getUsedSpace()).isEqualTo(900L);
        }

        @Test
        void shouldThrowWhenQuotaNotFound() {
            // given
            when(repository.findByUserIdWithLock(USER_ID))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.reserveSpace(USER_ID, 100L))
                    .isInstanceOf(StorageQuotaNotFoundException.class);
        }

        @Test
        void shouldReserveExactlyRemainingSpace() {
            // given
            StorageQuota quota = new StorageQuota();
            quota.setUsedSpace(800L);
            quota.setStorageLimit(1000L);

            when(repository.findByUserIdWithLock(USER_ID))
                    .thenReturn(Optional.of(quota));

            // when
            service.reserveSpace(USER_ID, 200L);

            // then
            assertThat(quota.getUsedSpace()).isEqualTo(1000L);
        }
    }

    @Nested
    class ReleaseSpace {

        @Test
        void shouldUpdateUsedSpaceCorrectly() {
            // given
            long bytesToRelease = 100L;
            StorageQuota existingQuota = new StorageQuota();
            existingQuota.setUserId(USER_ID);
            existingQuota.setUsedSpace(500L);
            existingQuota.setStorageLimit(1000L);
            when(repository.findByUserIdWithLock(USER_ID))
                    .thenReturn(Optional.of(existingQuota));

            // when
            service.releaseSpace(USER_ID, bytesToRelease);

            // then
            assertEquals(400L, existingQuota.getUsedSpace());
        }

        @Test
        void shouldThrowException_WhenQuotaNotFound() {
            // given
            long bytesToRelease = 100L;

            when(repository.findByUserIdWithLock(USER_ID))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.releaseSpace(USER_ID, bytesToRelease))
                    .isInstanceOf(StorageQuotaNotFoundException.class);
            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        void shouldSetZero_WithExactUsedSpace() {
            // given
            long bytesToRelease = 500L;

            StorageQuota existingQuota = new StorageQuota();
            existingQuota.setUserId(USER_ID);
            existingQuota.setUsedSpace(500L);
            existingQuota.setStorageLimit(1000L);

            when(repository.findByUserIdWithLock(USER_ID))
                    .thenReturn(Optional.of(existingQuota));

            // when
            service.releaseSpace(USER_ID, bytesToRelease);

            // then
            assertEquals(0L, existingQuota.getUsedSpace());
        }
    }
}
