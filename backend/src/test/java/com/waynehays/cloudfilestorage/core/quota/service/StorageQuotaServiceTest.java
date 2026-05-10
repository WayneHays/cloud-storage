package com.waynehays.cloudfilestorage.core.quota.service;

import com.waynehays.cloudfilestorage.core.quota.dto.SpaceReleaseDto;
import com.waynehays.cloudfilestorage.core.quota.entity.StorageQuota;
import com.waynehays.cloudfilestorage.core.quota.exception.QuotaLimitException;
import com.waynehays.cloudfilestorage.core.quota.exception.QuotaNotFoundException;
import com.waynehays.cloudfilestorage.core.quota.factory.StorageQuotaFactory;
import com.waynehays.cloudfilestorage.core.quota.repository.StorageQuotaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageQuotaServiceTest {

    @Mock
    private StorageQuotaFactory factory;

    @Mock
    private StorageQuotaRepository repository;

    @InjectMocks
    private StorageQuotaService service;

    private static final Long USER_ID = 1L;

    @Nested
    class CreateAndSaveQuota {

        @Test
        @DisplayName("Should create and save quota")
        void shouldCreateAndSaveQuota() {
            // given
            StorageQuota quota = new StorageQuota(USER_ID, 10000L);
            when(factory.create(USER_ID, 10000L)).thenReturn(quota);

            // when
            service.createStorageQuota(USER_ID, 10000L);

            // then
            verify(factory).create(USER_ID, 10000L);
            verify(repository).saveAndFlush(quota);
            assertThat(quota.getUsedSpace()).isEqualTo(0);
            assertThat(quota.getStorageLimit()).isEqualTo(10000L);
            assertThat(quota.getUserId()).isEqualTo(USER_ID);
        }
    }

    @Nested
    class ReserveSpace {

        @Test
        @DisplayName("Should reserve space when enough free space")
        void shouldReserveSpace_WhenEnoughFree() {
            // given
            StorageQuota quota = new StorageQuota(USER_ID, 1000L);
            quota.setUsedSpace(200L);

            when(repository.findByUserIdWithLock(USER_ID))
                    .thenReturn(Optional.of(quota));

            // when
            service.reserveSpace(USER_ID, 300L);

            // then
            assertThat(quota.getUsedSpace()).isEqualTo(500L);
        }

        @Test
        @DisplayName("Should throw when not enough space")
        void shouldThrow_WhenNotEnoughSpace() {
            // given
            StorageQuota quota = new StorageQuota(USER_ID, 1000L);
            quota.setUsedSpace(900L);

            when(repository.findByUserIdWithLock(USER_ID))
                    .thenReturn(Optional.of(quota));

            // when & then
            assertThatThrownBy(() -> service.reserveSpace(USER_ID, 200L))
                    .isInstanceOf(QuotaLimitException.class);
            assertThat(quota.getUsedSpace()).isEqualTo(900L);
        }

        @Test
        @DisplayName("Should throw when quota not found")
        void shouldThrow_WhenQuotaNotFound() {
            // given
            when(repository.findByUserIdWithLock(USER_ID))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.reserveSpace(USER_ID, 100L))
                    .isInstanceOf(QuotaNotFoundException.class);
        }

        @Test
        @DisplayName("Should reserve exactly remaining space")
        void shouldReserveExactlyRemainingSpace() {
            // given
            StorageQuota quota = new StorageQuota(USER_ID, 1000L);
            quota.setUsedSpace(800L);

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
        @DisplayName("Should release used space correctly")
        void shouldReleaseUsedSpaceCorrectly() {
            // given
            StorageQuota quota = new StorageQuota(USER_ID, 1000L);
            quota.setUsedSpace(500L);
            when(repository.findByUserIdWithLock(USER_ID))
                    .thenReturn(Optional.of(quota));

            // when
            service.releaseSpace(USER_ID, 100L);

            // then
            assertEquals(400L, quota.getUsedSpace());
        }

        @Test
        @DisplayName("Should throw when quota not found")
        void shouldThrow_WhenQuotaNotFound() {
            // given
            when(repository.findByUserIdWithLock(USER_ID))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.releaseSpace(USER_ID, 100L))
                    .isInstanceOf(QuotaNotFoundException.class);
            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Should set used space to zero when bytes to release exactly the same")
        void shouldSetZero_WithExactUsedSpace() {
            // given
            StorageQuota quota = new StorageQuota(USER_ID, 1000L);
            quota.setUsedSpace(500L);

            when(repository.findByUserIdWithLock(USER_ID))
                    .thenReturn(Optional.of(quota));

            // when
            service.releaseSpace(USER_ID, 500L);

            // then
            assertEquals(0L, quota.getUsedSpace());
        }
    }

    @Nested
    class BatchReleaseUsedSpace {

        @Test
        @DisplayName("Should split list into parallel arrays and pass to repository")
        void shouldSplitListIntoArrays() {
            // given
            List<SpaceReleaseDto> input = List.of(
                    new SpaceReleaseDto(1L, 300L),
                    new SpaceReleaseDto(2L, 500L),
                    new SpaceReleaseDto(3L, 800L)
            );

            // when
            service.batchReleaseUsedSpace(input);

            // then
            ArgumentCaptor<Long[]> userIdsCaptor = ArgumentCaptor.forClass(Long[].class);
            ArgumentCaptor<Long[]> bytesCaptor = ArgumentCaptor.forClass(Long[].class);
            verify(repository).batchReleaseUsedSpace(userIdsCaptor.capture(), bytesCaptor.capture());

            assertThat(userIdsCaptor.getValue()).containsExactly(1L, 2L, 3L);
            assertThat(bytesCaptor.getValue()).containsExactly(300L, 500L, 800L);
        }

        @Test
        @DisplayName("Should preserve order from input list")
        void shouldPreserveOrder() {
            // given
            List<SpaceReleaseDto> input = List.of(
                    new SpaceReleaseDto(42L, 999L),
                    new SpaceReleaseDto(7L, 100L)
            );

            // when
            service.batchReleaseUsedSpace(input);

            // then
            ArgumentCaptor<Long[]> userIdsCaptor = ArgumentCaptor.forClass(Long[].class);
            ArgumentCaptor<Long[]> bytesCaptor = ArgumentCaptor.forClass(Long[].class);
            verify(repository).batchReleaseUsedSpace(userIdsCaptor.capture(), bytesCaptor.capture());

            assertThat(userIdsCaptor.getValue()).containsExactly(42L, 7L);
            assertThat(bytesCaptor.getValue()).containsExactly(999L, 100L);
        }

        @Test
        @DisplayName("Should pass empty arrays when input is empty")
        void shouldPassEmptyArrays() {
            // when
            service.batchReleaseUsedSpace(List.of());

            // then
            ArgumentCaptor<Long[]> userIdsCaptor = ArgumentCaptor.forClass(Long[].class);
            ArgumentCaptor<Long[]> bytesCaptor = ArgumentCaptor.forClass(Long[].class);
            verify(repository).batchReleaseUsedSpace(userIdsCaptor.capture(), bytesCaptor.capture());

            assertThat(userIdsCaptor.getValue()).isEmpty();
            assertThat(bytesCaptor.getValue()).isEmpty();
        }
    }
}
