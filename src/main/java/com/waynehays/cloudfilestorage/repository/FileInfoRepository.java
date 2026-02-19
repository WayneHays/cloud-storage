package com.waynehays.cloudfilestorage.repository;

import com.waynehays.cloudfilestorage.entity.FileInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileInfoRepository extends JpaRepository<FileInfo, Long> {

    List<FileInfo> findByUserId(Long userId);

    Optional<FileInfo> findByUserIdAndName(Long userId, String name);
}
