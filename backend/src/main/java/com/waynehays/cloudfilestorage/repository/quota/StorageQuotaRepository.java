package com.waynehays.cloudfilestorage.repository.quota;

import com.waynehays.cloudfilestorage.entity.StorageQuota;
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
public interface StorageQuotaRepository extends JpaRepository<StorageQuota, Long>, StorageQuotaRepositoryCustom {

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
            FROM resource_metadata r
            WHERE r.user_id = q.user_id
              AND r.type = 'FILE'
              AND r.marked_for_deletion = false
        ), 0)
        WHERE q.user_id IN (?1)
        """, nativeQuery = true)
    void reconcileUsedSpace(List<Long> userIds);
}
