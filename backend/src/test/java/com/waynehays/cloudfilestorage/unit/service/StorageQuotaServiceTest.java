package com.waynehays.cloudfilestorage.unit.service;

import com.waynehays.cloudfilestorage.dto.internal.quota.SpaceReleaseDto;
import com.waynehays.cloudfilestorage.dto.internal.quota.StorageQuotaDto;
import com.waynehays.cloudfilestorage.dto.internal.quota.UsedSpaceCorrectionDto;
import com.waynehays.cloudfilestorage.entity.StorageQuota;
import com.waynehays.cloudfilestorage.exception.ResourceStorageLimitException;
import com.waynehays.cloudfilestorage.exception.StorageQuotaNotFoundException;
import com.waynehays.cloudfilestorage.mapper.StorageQuotaMapper;
import com.waynehays.cloudfilestorage.repository.quota.StorageQuotaRepository;
import com.waynehays.cloudfilestorage.service.quota.StorageQuotaService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageQuotaServiceTest {

    @Mock
    private StorageQuotaMapper mapper;

    @Mock
    private StorageQuotaRepository repository;

    @InjectMocks
    private StorageQuotaService service;

    private static final Long USER_ID = 1L;

    @Nested
    class CreateStorageQuota {

        @Test
        void shouldCreateAndSaveQuota() {
            // given
            StorageQuota entity = new StorageQuota();
            when(mapper.toEntity(USER_ID, 10000L)).thenReturn(entity);

            // when
            service.createStorageQuota(USER_ID, 10000L);

            // then
            verify(repository).save(entity);
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
        void shouldDelegateToRepository() {
            // when
            service.releaseSpace(USER_ID, 500L);

            // then
            verify(repository).decreaseUsedSpace(USER_ID, 500L);
        }
    }

    @Nested
    class FindAllQuotas {

        @Test
        void shouldReturnMappedPage() {
            // given
            StorageQuota entity = new StorageQuota();
            StorageQuotaDto dto = new StorageQuotaDto(USER_ID, 200L, 1000L);
            Page<StorageQuota> page = new PageImpl<>(List.of(entity));

            when(repository.findAll(any(Pageable.class))).thenReturn(page);
            when(mapper.toDto(entity)).thenReturn(dto);

            // when
            Page<StorageQuotaDto> result = service.findAllQuotas(PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst()).isEqualTo(dto);
        }
    }

    @Nested
    class BatchUpdateUsedSpace {

        @Test
        void shouldConvertToParamsAndDelegate() {
            // given
            List<UsedSpaceCorrectionDto> corrections = List.of(
                    new UsedSpaceCorrectionDto(1L, 500L),
                    new UsedSpaceCorrectionDto(2L, 300L)
            );

            // when
            service.batchUpdateUsedSpace(corrections);

            // then
            verify(repository).batchUpdateUsedSpace(argThat(params -> {
                Object[] first = params.get(0);
                Object[] second = params.get(1);
                return params.size() == 2
                        && first[0].equals(500L) && first[1].equals(1L)
                        && second[0].equals(300L) && second[1].equals(2L);
            }));
        }
    }

    @Nested
    class BatchDecreaseUsedSpace {

        @Test
        void shouldConvertToParamsAndDelegate() {
            // given
            List<SpaceReleaseDto> releases = List.of(
                    new SpaceReleaseDto(1L, 100L),
                    new SpaceReleaseDto(2L, 200L)
            );

            // when
            service.batchDecreaseUsedSpace(releases);

            // then
            verify(repository).batchDecreaseUsedSpace(argThat(params -> {
                Object[] first = params.get(0);
                Object[] second = params.get(1);
                return params.size() == 2
                        && first[0].equals(100L) && first[1].equals(1L)
                        && second[0].equals(200L) && second[1].equals(2L);
            }));
        }
    }
}
