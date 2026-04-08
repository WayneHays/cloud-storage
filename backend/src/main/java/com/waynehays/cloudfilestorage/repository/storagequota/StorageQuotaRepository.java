package com.waynehays.cloudfilestorage.repository.storagequota;

import com.waynehays.cloudfilestorage.entity.StorageQuota;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StorageQuotaRepository extends JpaRepository<StorageQuota, Long>, StorageQuotaRepositoryCustom {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT q
            FROM StorageQuota q
            WHERE q.userId = :userId
            """)
    Optional<StorageQuota> findByUserIdWithLock(@Param("userId") Long userId);


    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE StorageQuota q
            SET q.usedSpace = GREATEST(0, q.usedSpace - :bytes)
            WHERE q.userId = :userId
            """)
    void decreaseUsedSpace(@Param("userId") Long userId,
                           @Param("bytes") long bytes);
}
