package com.waynehays.cloudfilestorage.integration.repository;

import com.waynehays.cloudfilestorage.entity.FileInfo;
import com.waynehays.cloudfilestorage.entity.User;
import com.waynehays.cloudfilestorage.integration.base.AbstractRepositoryIntegrationTest;
import com.waynehays.cloudfilestorage.repository.FileInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FileInfoRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {
    private static final String DIRECTORY = "docs";
    private static final String FILENAME = "file.txt";
    private static final String STORAGE_KEY = "user-1-files/docs/file.txt";
    private static final String CONTENT_TYPE = "text/plain";
    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "password";
    private static final long SIZE = 1024L;

    @Autowired
    private FileInfoRepository fileInfoRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;

    @BeforeEach
    void setUp() {
        user = entityManager.persist(User.builder()
                .username(USERNAME)
                .password(PASSWORD)
                .build());

        entityManager.persistAndFlush(FileInfo.builder()
                .directory(DIRECTORY)
                .name(FILENAME)
                .storageKey(STORAGE_KEY)
                .size(SIZE)
                .contentType(CONTENT_TYPE)
                .user(user)
                .build());

        entityManager.clear();
    }

    @Test
    @DisplayName("Should find file by userId, directory and name")
    void shouldFindFile() {
        // when
        Optional<FileInfo> result = fileInfoRepository.findByUserIdAndDirectoryAndName(user.getId(), DIRECTORY, FILENAME);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo(FILENAME);
    }

    @Test
    @DisplayName("Should return empty when file not found")
    void shouldReturnEmpty_whenFileNotFound() {
        // when
        Optional<FileInfo> result = fileInfoRepository.findByUserIdAndDirectoryAndName(
                user.getId(), DIRECTORY, "sdghjsfdg");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should delete existing file")
    void shouldDeleteExistingFile() {
        // when
        fileInfoRepository.deleteByUserIdAndDirectoryAndName(
                user.getId(), DIRECTORY, FILENAME);

        // then
        assertThat(fileInfoRepository.findByUserIdAndDirectoryAndName(
                user.getId(), DIRECTORY, FILENAME)).isEmpty();
    }
}
