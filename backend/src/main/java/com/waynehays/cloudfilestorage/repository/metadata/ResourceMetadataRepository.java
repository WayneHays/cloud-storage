package com.waynehays.cloudfilestorage.repository.metadata;

import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.entity.ResourceType;
import com.waynehays.cloudfilestorage.dto.internal.quota.UsedSpace;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
            AND r.parentPath = :parentPath
            AND r.markedForDeletion = false
            """)
    List<ResourceMetadata> findByParentPath(@Param("userId") Long userId,
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

    @Query("""
            SELECT r
            FROM ResourceMetadata r
            WHERE r.type = 'FILE'
            AND r.markedForDeletion = true
            """)
    List<ResourceMetadata> findFilesMarkedForDeletion(Pageable pageable);

    @Query("""
            SELECT r FROM ResourceMetadata r
            WHERE r.userId =:userId
            AND r.path LIKE CONCAT(:prefix, '%')
            AND r.type = 'FILE'
            AND r.markedForDeletion = false
            """)
    List<ResourceMetadata> findFilesByPathPrefix(@Param("userId") Long userId,
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
            SELECT r.userId AS userId, COALESCE(SUM(r.size), 0) AS totalSize
            FROM ResourceMetadata r
            WHERE r.type = :type
            AND userId IN :userIds
            GROUP BY r.userId
            """)
    List<UsedSpace> sumFileSizesGroupByUserId(@Param("userIds") List<Long> userIds,
                                              @Param("type") ResourceType type);
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE ResourceMetadata r
            SET r.markedForDeletion = true
            WHERE r.userId = :userId
            AND r.path = :path
            """)
    void markForDeletionByPath(@Param("userId") Long userId,
                               @Param("path") String path);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE ResourceMetadata r
            SET r.path = CONCAT(:prefixTo, SUBSTRING(r.path, LENGTH(:prefixFrom) + 1)),
                r.parentPath = CONCAT(:prefixTo, SUBSTRING(r.parentPath, LENGTH(:prefixFrom) + 1))
            WHERE r.userId = :userId
            AND r.path LIKE CONCAT(:prefixFrom, '%')
            """)
    void updatePathsByPathPrefix(@Param("userId") Long userId,
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
    void deleteByPathPrefix(@Param("userId") Long userId,
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
            WHERE r.id IN :ids
            """)
    void deleteByIds(@Param("ids") List<Long> ids);
}
