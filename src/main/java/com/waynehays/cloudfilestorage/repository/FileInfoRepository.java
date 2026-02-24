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

    List<FileInfo> findByUserIdAndNameContainingIgnoreCase(Long userId, String name);

    @Query("SELECT f.storageKey FROM FileInfo f WHERE f.user.id = :userId AND f.directory = :directory AND f.name = :name")
    Optional<String> findStorageKeyByUserIdAndDirectoryAndName(@Param("userId") Long userId,
                                                               @Param("directory") String directory,
                                                               @Param("name") String name);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM FileInfo f WHERE f.user.id = :userId AND f.directory = :directory AND f.name = :name")
    void deleteByUserIdAndDirectoryAndName(@Param("userId") Long userId,
                                           @Param("directory") String directory,
                                           @Param("name") String name);
}
