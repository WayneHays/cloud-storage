package com.waynehays.cloudfilestorage.repository;

import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceMetadataRepository extends JpaRepository<ResourceMetadata, Long> {

    @Query("SELECT COALESCE(SUM (r.size), 0) FROM ResourceMetadata r WHERE r.userId = :userId AND r.type = :type")
    long sumSizeByUserId(@Param("userId") Long userId, @Param("type") ResourceType type);

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
}
