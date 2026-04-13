package com.waynehays.cloudfilestorage.integration.repository;

import com.waynehays.cloudfilestorage.dto.internal.quota.SpaceCorrectionDto;
import com.waynehays.cloudfilestorage.dto.internal.quota.SpaceReleaseDto;
import com.waynehays.cloudfilestorage.entity.StorageQuota;
import com.waynehays.cloudfilestorage.repository.quota.StorageQuotaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StorageQuotaRepository integration tests")
class StorageQuotaRepositoryTest extends AbstractRepositoryTest {
    private static final long TEST_STORAGE_LIMIT = 100000;

    @Autowired
    private StorageQuotaRepository repository;

    private StorageQuota quota(Long userId, long usedSpace) {
        StorageQuota quota = new StorageQuota();
        quota.setUserId(userId);
        quota.setUsedSpace(usedSpace);
        quota.setStorageLimit(TEST_STORAGE_LIMIT);
        return quota;
    }

    @Nested
    @DisplayName("findByUserIdWithLock")
    class FindByUserIdWithLock {

        @Test
        @DisplayName("should return quota for given user")
        void shouldReturnQuota() {
            // given
            em.persistAndFlush(quota(userId, 500L));
            em.clear();

            // when
            Optional<StorageQuota> result = repository.findByUserIdWithLock(userId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getUsedSpace()).isEqualTo(500L);
        }

        @Test
        @DisplayName("should return empty when quota does not exist")
        void shouldReturnEmpty() {
            // when
            Optional<StorageQuota> result = repository.findByUserIdWithLock(userId);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should not return other user's quota")
        void shouldRespectUserIsolation() {
            // given
            em.persistAndFlush(quota(otherUserId, 300L));
            em.clear();

            // when
            Optional<StorageQuota> result = repository.findByUserIdWithLock(userId);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("decreaseUsedSpace")
    class DecreaseUsedSpace {

        @Test
        @DisplayName("should decrease used space by given amount")
        void shouldDecrease() {
            // given
            em.persistAndFlush(quota(userId, 1000L));

            // when
            repository.decreaseUsedSpace(userId, 300L);
            em.clear();

            // then
            StorageQuota updated = repository.findByUserIdWithLock(userId).orElseThrow();
            assertThat(updated.getUsedSpace()).isEqualTo(700L);
        }

        @Test
        @DisplayName("should clamp used space at zero")
        void shouldClampAtZero() {
            // given
            em.persistAndFlush(quota(userId, 100L));

            // when
            repository.decreaseUsedSpace(userId, 500L);
            em.clear();

            // then
            StorageQuota updated = repository.findByUserIdWithLock(userId).orElseThrow();
            assertThat(updated.getUsedSpace()).isZero();
        }

        @Test
        @DisplayName("should not affect other user's quota")
        void shouldNotAffectOtherUsers() {
            // given
            em.persist(quota(userId, 1000L));
            em.persist(quota(otherUserId, 2000L));
            em.flush();

            // when
            repository.decreaseUsedSpace(userId, 300L);
            em.clear();

            // then
            StorageQuota other = repository.findByUserIdWithLock(otherUserId).orElseThrow();
            assertThat(other.getUsedSpace()).isEqualTo(2000L);
        }
    }

    @Nested
    @DisplayName("batchUpdateUsedSpace")
    class BatchUpdateUsedSpace {

        @Test
        @DisplayName("should update used space for multiple users")
        void shouldUpdateMultiple() {
            // given
            em.persist(quota(userId, 100L));
            em.persist(quota(otherUserId, 200L));
            em.flush();
            em.clear();

            // when
            repository.batchUpdateUsedSpace(List.of(
                    new SpaceCorrectionDto(userId, 500L),
                    new SpaceCorrectionDto(otherUserId, 800L)
            ));

            // then
            assertThat(repository.findByUserIdWithLock(userId).orElseThrow().getUsedSpace())
                    .isEqualTo(500L);
            assertThat(repository.findByUserIdWithLock(otherUserId).orElseThrow().getUsedSpace())
                    .isEqualTo(800L);
        }

        @Test
        @DisplayName("should update single user's quota")
        void shouldUpdateSingle() {
            // given
            em.persist(quota(userId, 100L));
            em.persist(quota(otherUserId, 200L));
            em.flush();
            em.clear();

            // when
            repository.batchUpdateUsedSpace(List.of(
                    new SpaceCorrectionDto(userId, 999L)
            ));

            // then
            assertThat(repository.findByUserIdWithLock(userId).orElseThrow().getUsedSpace())
                    .isEqualTo(999L);
            assertThat(repository.findByUserIdWithLock(otherUserId).orElseThrow().getUsedSpace())
                    .isEqualTo(200L);
        }
    }


    @Nested
    @DisplayName("batchDecreaseUsedSpace")
    class BatchDecreaseUsedSpace {

        @Test
        @DisplayName("should decrease used space for multiple users")
        void shouldDecreaseMultiple() {
            // given
            em.persist(quota(userId, 1000L));
            em.persist(quota(otherUserId, 2000L));
            em.flush();
            em.clear();

            // when
            repository.batchDecreaseUsedSpace(List.of(
                    new SpaceReleaseDto(userId, 300L),
                    new SpaceReleaseDto(otherUserId, 500L)
            ));

            // then
            assertThat(repository.findByUserIdWithLock(userId).orElseThrow().getUsedSpace())
                    .isEqualTo(700L);
            assertThat(repository.findByUserIdWithLock(otherUserId).orElseThrow().getUsedSpace())
                    .isEqualTo(1500L);
        }

        @Test
        @DisplayName("should clamp used space at zero")
        void shouldClampAtZero() {
            // given
            em.persist(quota(userId, 100L));
            em.flush();
            em.clear();

            // when
            repository.batchDecreaseUsedSpace(List.of(
                    new SpaceReleaseDto(userId, 500L)
            ));

            // then
            assertThat(repository.findByUserIdWithLock(userId).orElseThrow().getUsedSpace())
                    .isZero();
        }

        @Test
        @DisplayName("should only affect users in the batch")
        void shouldOnlyAffectBatchUsers() {
            // given
            em.persist(quota(userId, 1000L));
            em.persist(quota(otherUserId, 2000L));
            em.flush();
            em.clear();

            // when
            repository.batchDecreaseUsedSpace(List.of(
                    new SpaceReleaseDto(userId, 300L)
            ));

            // then
            assertThat(repository.findByUserIdWithLock(userId).orElseThrow().getUsedSpace())
                    .isEqualTo(700L);
            assertThat(repository.findByUserIdWithLock(otherUserId).orElseThrow().getUsedSpace())
                    .isEqualTo(2000L);
        }
    }
}
