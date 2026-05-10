package com.waynehays.cloudfilestorage.core.quota.repository;

import com.waynehays.cloudfilestorage.AbstractRepositoryTest;
import com.waynehays.cloudfilestorage.core.metadata.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.core.quota.entity.StorageQuota;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class StorageQuotaRepositoryTest extends AbstractRepositoryTest {
    private static final long TEST_STORAGE_LIMIT = 100000;

    @Autowired
    private StorageQuotaRepository repository;

    private void persistQuota(Long userId, long usedSpace) {
        StorageQuota quota = new StorageQuota(userId, TEST_STORAGE_LIMIT);
        quota.setUsedSpace(usedSpace);
        em.persistAndFlush(quota);
    }

    private long findUsedSpace(Long userId) {
        String sql = """
                SELECT q.usedSpace
                FROM StorageQuota q
                WHERE q.userId = :userId
                """;
        return em.getEntityManager()
                .createQuery(sql, Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
    }

    @Nested
    class FindAllUserIds {

        @Test
        @DisplayName("Should return page of user ids")
        void shouldReturnPageOfUserIds() {
            // given
            persistQuota(userId, 100);
            persistQuota(otherUserId, 200);

            // when
            Page<Long> result = repository.findAllUserIds(PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).containsExactlyInAnyOrder(userId, otherUserId);
        }

        @Test
        @DisplayName("Should return correct page size")
        void shouldReturnCorrectPageSize() {
            // given
            persistQuota(userId, 100);
            persistQuota(otherUserId, 200);

            // when
            Page<Long> firstPage = repository.findAllUserIds(PageRequest.of(0, 1));

            // then
            assertThat(firstPage.getContent()).hasSize(1);
            assertThat(firstPage.hasNext()).isTrue();
        }

        @Test
        @DisplayName("Should return empty page when no quotas exist")
        void shouldReturnEmptyPageWhenNoQuotasExist() {
            // when
            Page<Long> result = repository.findAllUserIds(PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.hasNext()).isFalse();
        }
    }

    @Nested
    class FindByUserIdWithLock {

        @Test
        @DisplayName("should return quota for given user")
        void shouldReturnQuota() {
            // given
            persistQuota(userId, 500L);
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
            persistQuota(otherUserId, 300L);
            em.clear();

            // when
            Optional<StorageQuota> result = repository.findByUserIdWithLock(userId);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class BatchReleaseUsedSpace {

        @Test
        @DisplayName("should decrease used space for multiple users")
        void shouldReleaseSpaceForMultipleUsers() {
            // given
            persistQuota(userId, 1000L);
            persistQuota(otherUserId, 2000L);
            em.clear();

            // when
            repository.batchReleaseUsedSpace(
                    new Long[]{userId, otherUserId},
                    new Long[]{300L, 500L}
            );

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
            persistQuota(userId, 100L);
            em.clear();

            // when
            repository.batchReleaseUsedSpace(
                    new Long[]{userId},
                    new Long[]{500L}
            );

            // then
            assertThat(repository.findByUserIdWithLock(userId).orElseThrow().getUsedSpace())
                    .isZero();
        }

        @Test
        @DisplayName("should only affect users in the batch")
        void shouldOnlyAffectBatchUsers() {
            // given
            persistQuota(userId, 1000L);
            persistQuota(otherUserId, 2000L);
            em.clear();

            // when
            repository.batchReleaseUsedSpace(
                    new Long[]{userId},
                    new Long[]{300L}
            );

            // then
            assertThat(repository.findByUserIdWithLock(userId).orElseThrow().getUsedSpace())
                    .isEqualTo(700L);
            assertThat(repository.findByUserIdWithLock(otherUserId).orElseThrow().getUsedSpace())
                    .isEqualTo(2000L);
        }
    }

    @Nested
    class ReconcileStorageQuotas {

        @Test
        @DisplayName("Should set used_space to sum of file sizes")
        void shouldSetUsedSpaceToSumOfFileSizes() {
            // given
            persistQuota(userId, 0);
            persistFile(userId, "key1", "docs/a.txt", 100);
            persistFile(userId, "key2", "docs/b.txt", 250);

            // when
            repository.reconcileUsedSpace(List.of(userId));

            // then
            assertThat(findUsedSpace(userId)).isEqualTo(350);
        }

        @Test
        @DisplayName("Should correct overestimated used_space")
        void shouldCorrectOverestimatedUsedSpace() {
            // given
            persistQuota(userId, 500);
            persistFile(userId, "key1", "file.txt", 100);

            // when
            repository.reconcileUsedSpace(List.of(userId));

            // then
            assertThat(findUsedSpace(userId)).isEqualTo(100);
        }

        @Test
        @DisplayName("Should correct underestimated used_space")
        void shouldCorrectUnderestimatedUsedSpace() {
            // given
            persistQuota(userId, 50);
            persistFile(userId, "key1", "a.txt", 100);
            persistFile(userId, "key2", "b.txt", 200);

            // when
            repository.reconcileUsedSpace(List.of(userId));

            // then
            assertThat(findUsedSpace(userId)).isEqualTo(300);
        }

        @Test
        @DisplayName("Should set used_space to zero when user has no files")
        void shouldSetUsedSpaceToZeroWhenNoFiles() {
            // given
            persistQuota(userId, 500);

            // when
            repository.reconcileUsedSpace(List.of(userId));

            // then
            assertThat(findUsedSpace(userId)).isEqualTo(0);
        }

        @Test
        @DisplayName("Should exclude files marked for deletion from sum")
        void shouldExcludeFilesMarkedForDeletion() {
            // given
            persistQuota(userId, 0);
            persistFile(userId, "key1", "active.txt", 100);

            ResourceMetadata metadata = file(userId, "key2", "deleted.txt", 500);
            metadata.setMarkedForDeletion(true);
            em.persist(metadata);
            em.flush();

            // when
            repository.reconcileUsedSpace(List.of(userId));

            // then
            assertThat(findUsedSpace(userId)).isEqualTo(100);
        }

        @Test
        @DisplayName("Should exclude directories from sum")
        void shouldExcludeDirectoriesFromSum() {
            // given
            persistQuota(userId, 0);
            persistDirectory(userId, "docs/");
            persistFile(userId, "key1", "docs/file.txt", 100);

            // when
            repository.reconcileUsedSpace(List.of(userId));

            // then
            assertThat(findUsedSpace(userId)).isEqualTo(100);
        }

        @Test
        @DisplayName("Should reconcile multiple users in one call")
        void shouldReconcileMultipleUsers() {
            // given
            persistQuota(userId, 0);
            persistQuota(otherUserId, 999);
            persistFile(userId, "key1", "a.txt", 100);
            persistFile(otherUserId, "key1", "b.txt", 200);

            // when
            repository.reconcileUsedSpace(List.of(userId, otherUserId));

            // then
            assertThat(findUsedSpace(userId)).isEqualTo(100);
            assertThat(findUsedSpace(otherUserId)).isEqualTo(200);
        }

        @Test
        @DisplayName("Should not affect users not in the list")
        void shouldNotAffectUsersNotInList() {
            // given
            persistQuota(userId, 0);
            persistQuota(otherUserId, 999);
            persistFile(userId, "key1", "a.txt", 100);

            // when
            repository.reconcileUsedSpace(List.of(userId));

            // then
            assertThat(findUsedSpace(otherUserId)).isEqualTo(999);
        }
    }
}