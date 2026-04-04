package com.waynehays.cloudfilestorage.repository;

import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.service.storagequota.UsedSpace;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ResourceMetadataRepository extends JpaRepository<ResourceMetadata, Long>, ResourceMetadataRepositoryCustom {

    @Query("""
            SELECT r FROM ResourceMetadata r
            WHERE r.userId = :userId
            AND r.path = :path
            AND r.markedForDeletion = false
            """)
    Optional<ResourceMetadata> findByPath(@Param("userId") Long userId,
                                          @Param("path") String path);

    @Query("""
            SELECT r FROM ResourceMetadata r
            WHERE r.userId = :userId
            AND r.path LIKE CONCAT(:prefix, '%')
            AND r.markedForDeletion = false
            """)
    List<ResourceMetadata> findAllByPrefix(@Param("userId") Long userId,
                                           @Param("prefix") String prefix);

    @Query("""
            SELECT r FROM ResourceMetadata r
            WHERE r.userId = :userId
            AND r.parentPath = :parentPath
            AND r.markedForDeletion = false
            """)
    List<ResourceMetadata> findDirectChildren(@Param("userId") Long userId,
                                              @Param("parentPath") String parentPath);

    @Query("""
            SELECT r FROM ResourceMetadata r
            WHERE r.userId = :userId
            AND LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%'))
            AND r.markedForDeletion = false
            """)
    List<ResourceMetadata> findByNameContaining(@Param("userId") Long userId,
                                                @Param("name") String name,
                                                Pageable pageable);

    List<ResourceMetadata> findByMarkedForDeletionTrue(Pageable pageable);

    @Query("""
            SELECT r FROM ResourceMetadata r
            WHERE r.userId =:userId
            AND r.path LIKE CONCAT(:prefix, '%')
            AND r.type = 'FILE'
            AND r.markedForDeletion = false
            """)
    List<ResourceMetadata> findFilesByPrefix(@Param("userId") Long userId,
                                             @Param("prefix") String prefix);

    @Query("""
            SELECT r.path
            FROM ResourceMetadata r
            WHERE r.userId = :userId
            AND r.path IN :paths
            AND r.markedForDeletion = false
            """)
    Set<String> findExistingPaths(@Param("userId") Long userId, @Param("paths") Set<String> paths);

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
            SELECT r.userId AS userId, COALESCE(SUM(r.size), 0) AS totalSize
            FROM ResourceMetadata r
            WHERE r.type = :type AND userId IN :userIds
            GROUP BY r.userId
            """)
    List<UsedSpace> sumSizeGroupByUserId(@Param("userIds") List<Long> userIds,
                                         @Param("type") ResourceType type);

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
            AND r.path LIKE CONCAT(:prefix, '%')
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

    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM ResourceMetadata r
            WHERE r.userId = :userId
            AND r.path = :path
            """)
    void deleteByPath(@Param("userId") Long userId,
                      @Param("path") String path);

    @Modifying(clearAutomatically = true)
    @Query("""
             DELETE FROM ResourceMetadata r
             WHERE r.userId = :userId
             AND r.path LIKE CONCAT(:prefix, '%')
            """)
    void deleteByPrefix(@Param("userId") Long userId,
                        @Param("prefix") String prefix);

    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM ResourceMetadata r
            WHERE r.userId = :userId
            AND r.path IN :paths
            """)
    void deleteByPaths(@Param("userId") Long userId,
                       @Param("paths") List<String> paths);

    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM ResourceMetadata r
            WHERE r.updatedAt < :threshold
            AND r.markedForDeletion = true
            """)
    int deleteStaleDeletionRecords(@Param("threshold") Instant threshold);
}
