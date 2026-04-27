package com.waynehays.cloudfilestorage.core.metadata;

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
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM ResourceMetadata r
            WHERE r.userId = :userId
            AND r.normalizedPath = :normalizedPath
            """)
    boolean existsByNormalizedPath(@Param("userId") Long userId,
                                   @Param("normalizedPath") String normalizedPath);

    @Query("""
            SELECT r FROM ResourceMetadata r
            WHERE r.userId = :userId
            AND r.normalizedPath = :normalizedPath
            AND r.markedForDeletion = false
            """)
    Optional<ResourceMetadata> findByNormalizedPath(@Param("userId") Long userId,
                                                    @Param("normalizedPath") String normalizedPath);

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
            WHERE r.userId = :userId
            AND r.normalizedPath LIKE CONCAT(:normalizedPrefix, '%')
            AND r.type = 'FILE'
            AND r.markedForDeletion = false
            """)
    List<ResourceMetadata> findFilesByPathPrefix(@Param("userId") Long userId,
                                                 @Param("normalizedPrefix") String normalizedPrefix);

    @Query("""
            SELECT r.path
            FROM ResourceMetadata r
            WHERE r.userId = :userId
            AND r.normalizedPath IN :normalizedPaths
            AND r.markedForDeletion = false
            """)
    Set<String> findExistingPaths(@Param("userId") Long userId,
                                  @Param("normalizedPaths") Set<String> normalizedPaths);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE ResourceMetadata r
            SET r.markedForDeletion = true
            WHERE r.userId = :userId
            AND r.normalizedPath = :normalizedPath
            """)
    void markForDeletionByNormalizedPath(@Param("userId") Long userId,
                                         @Param("normalizedPath") String normalizedPath);

    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM ResourceMetadata r
            WHERE r.userId = :userId
            AND r.normalizedPath = :normalizedPath
            AND r.type = 'FILE'
            """)
    void deleteFileByNormalizedPath(@Param("userId") Long userId,
                                    @Param("normalizedPath") String normalizedPath);

    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM ResourceMetadata r
            WHERE r.userId = :userId
            AND r.normalizedPath LIKE CONCAT(:normalizedPrefix, '%')
            """)
    void deleteByNormalizedPathPrefix(@Param("userId") Long userId,
                                      @Param("normalizedPrefix") String normalizedPrefix);

    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM ResourceMetadata r
            WHERE r.userId = :userId
            AND r.normalizedPath IN :normalizedPaths
            """)
    void deleteByNormalizedPaths(@Param("userId") Long userId,
                                 @Param("normalizedPaths") List<String> normalizedPaths);

    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM ResourceMetadata r
            WHERE r.id IN :ids
            """)
    void deleteByIds(@Param("ids") List<Long> ids);

    @Modifying(clearAutomatically = true)
    @Query(value = """
        UPDATE resource_metadata
        SET
            path = :pathTo || SUBSTRING(path FROM LENGTH(:normalizedPathFrom) + 1),
            normalized_path = LOWER(:pathTo || SUBSTRING(path FROM LENGTH(:normalizedPathFrom) + 1)),
            parent_path = CASE
                WHEN normalized_path = :normalizedPathFrom THEN :targetParentPath
                ELSE LOWER(:pathTo) || SUBSTRING(parent_path FROM LENGTH(:normalizedPathFrom) + 1)
            END,
            name = CASE
                WHEN normalized_path = :normalizedPathFrom THEN :targetName
                ELSE name
            END
        WHERE user_id = :userId
          AND (normalized_path = :normalizedPathFrom
               OR normalized_path LIKE :normalizedPathFrom || '%')
        """, nativeQuery = true)
    int moveMetadata(
            @Param("userId") Long userId,
            @Param("normalizedPathFrom") String normalizedPathFrom,
            @Param("pathTo") String pathTo,
            @Param("targetParentPath") String targetParentPath,
            @Param("targetName") String targetName
    );
}
