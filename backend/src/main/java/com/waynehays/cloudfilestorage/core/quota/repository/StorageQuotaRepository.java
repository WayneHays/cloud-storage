package com.waynehays.cloudfilestorage.core.quota.repository;

import com.waynehays.cloudfilestorage.core.quota.entity.StorageQuota;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StorageQuotaRepository extends JpaRepository<StorageQuota, Long> {

    @Query("""
            SELECT q.userId
            FROM StorageQuota q
            """)
    Page<Long> findAllUserIds(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT q
            FROM StorageQuota q
            WHERE q.userId = :userId
            """)
    Optional<StorageQuota> findByUserIdWithLock(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query(value = """
        UPDATE storage_quotas q
        SET used_space = COALESCE((
            SELECT SUM(r.size)
            FROM resource_metadatas r
            WHERE r.user_id = q.user_id
              AND r.type = 'FILE'
              AND r.marked_for_deletion = false
        ), 0)
        WHERE q.user_id IN (?1)
        """, nativeQuery = true)
    void reconcileUsedSpace(List<Long> userIds);

    @Modifying(clearAutomatically = true)
    @Query(value = """
        UPDATE storage_quotas q
        SET used_space = GREATEST(0, used_space - r.bytes)
        FROM (
            SELECT unnest(:userIds) AS user_id,
                   unnest(:bytesToRelease) AS bytes
        ) AS r
        WHERE q.user_id = r.user_id
        """, nativeQuery = true)
    void batchReleaseUsedSpace(@Param("userIds") Long[] userIds,
                               @Param("bytesToRelease") Long[] bytesToRelease);
}
