package com.waynehays.cloudfilestorage.integration.repository;

import com.waynehays.cloudfilestorage.entity.User;
import com.waynehays.cloudfilestorage.integration.base.AbstractRepositoryBaseTest;
import com.waynehays.cloudfilestorage.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest extends AbstractRepositoryBaseTest {
    private static final String USERNAME = "test-name";
    private static final String PASSWORD = "password";

    @Autowired
    private UserRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Should find user when exists")
    void shouldFindUser() {
        // given
        User user = new User();
        user.setUsername(USERNAME);
        user.setPassword(PASSWORD);
        user.setStorageLimit(10L);

        entityManager.persistAndFlush(user);
        entityManager.clear();

        // when
        Optional<User> result = repository.findByUsername(user.getUsername());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo(user.getUsername());
    }

    @Test
    @DisplayName("Should return empty when not found")
    void shouldReturnEmpty_whenNotFound() {
        // when
        Optional<User> result = repository.findByUsername("not exists");

        // then
        assertThat(result).isEmpty();
    }
}
