package com.waynehays.cloudfilestorage.repository;

import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.service.storagequota.UsedSpace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ResourceMetadataRepository extends JpaRepository<ResourceMetadata, Long> {

    @Query("""
            SELECT COALESCE(SUM(r.size), 0)
            FROM ResourceMetadata r
            WHERE r.userId = :userId
            AND r.path LIKE CONCAT(:prefix, '%')
            AND r.type = :type
            """)
    long sumSizeByPrefix(@Param("userId") Long userId,
                         @Param("prefix") String prefix,
                         @Param("type") ResourceType type);

    @Query("""
            SELECT r.path FROM ResourceMetadata r
            WHERE r.userId = :userId
            AND r.path IN :paths
            AND r.markedForDeletion = false
            """)
    Set<String> findExistingPaths(@Param("userId") Long userId, @Param("paths") Set<String> paths);

    @Query("""
            SELECT r.userId AS userId, COALESCE(SUM(r.size), 0) AS totalSize
            FROM ResourceMetadata r
            WHERE r.type = :type AND userId IN :userIds
            GROUP BY r.userId
            """)
    List<UsedSpace> sumSizeGroupByUserId(@Param("userIds") List<Long> userIds,
                                         @Param("type") ResourceType type);

    Optional<ResourceMetadata> findByUserIdAndPathAndMarkedForDeletionFalse(Long userId, String path);

    List<ResourceMetadata> findByUserIdAndPathStartingWithAndMarkedForDeletionFalse(Long userId, String pathPrefix);

    List<ResourceMetadata> findByUserIdAndParentPathAndMarkedForDeletionFalse(Long userId, String parentPath);

    List<ResourceMetadata> findByUserIdAndNameContainingIgnoreCaseAndMarkedForDeletionFalse(Long userId, String query);

    List<ResourceMetadata> findByMarkedForDeletionTrue();

    boolean existsByUserIdAndPathAndMarkedForDeletionFalse(Long userId, String path);

    void deleteByUserIdAndPath(Long userId, String path);

    void deleteByUserIdAndPathStartingWith(Long userId, String pathPrefix);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE ResourceMetadata r
            SET r.markedForDeletion = true
            WHERE r.userId = :userId
            AND r.path = :path
            """)
    void markForDeletion(@Param("userId") Long userId,
                         @Param("path") String path);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE ResourceMetadata r
            SET r.markedForDeletion = true
            WHERE r.userId = :userId
            AND r.path LIKE :prefix%
            """)
    void markForDeletionByPrefix(@Param("userId") Long userId,
                                 @Param("prefix") String prefix);


    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE ResourceMetadata r
            SET r.path = CONCAT(:prefixTo, SUBSTRING(r.path, LENGTH(:prefixFrom) + 1)),
                r.parentPath = CONCAT(:prefixTo, SUBSTRING(r.parentPath, LENGTH(:prefixFrom) + 1))
            WHERE r.userId = :userId
            AND r.path LIKE CONCAT(:prefixFrom, '%')
            """)
    void updatePathsByPrefix(@Param("userId") Long userId,
                             @Param("prefixFrom") String prefixFrom,
                             @Param("prefixTo") String prefixTo);
}
