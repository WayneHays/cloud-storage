package com.waynehays.cloudfilestorage.repository;

import com.waynehays.cloudfilestorage.entity.FileInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileInfoRepository extends JpaRepository<FileInfo, Long> {

    Optional<FileInfo> findByUserIdAndDirectoryAndName(Long userId, String directory, String name);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM FileInfo f WHERE f.user.id = :userId AND f.directory = :directory AND f.name = :name")
    void deleteByUserIdAndDirectoryAndName(@Param("userId") Long userId,
                                           @Param("directory") String directory,
                                           @Param("name") String name);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            DELETE FROM files_info
            WHERE user_id = :userId AND directory = :directory AND name = :name
            RETURNING storage_key
            """, nativeQuery = true)
    Optional<String> deleteAndReturnStorageKey(@Param("userId") Long userId,
                                               @Param("directory") String directory,
                                               @Param("name") String name);

    @Query("""
            SELECT f FROM FileInfo f
            WHERE f.user.id = :userId
            AND (f.directory = :directory OR f.directory LIKE CONCAT(:directory, '/%'))
            """)
    List<FileInfo> findByUserIdAndDirectoryRecursive(Long userId, String directory);

    List<FileInfo> findByUserId(Long userId);
}
