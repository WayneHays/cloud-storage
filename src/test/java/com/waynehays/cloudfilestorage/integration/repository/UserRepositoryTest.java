package com.waynehays.cloudfilestorage.integration.repository;

import com.waynehays.cloudfilestorage.entity.User;
import com.waynehays.cloudfilestorage.integration.base.AbstractRepositoryTest;
import com.waynehays.cloudfilestorage.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest extends AbstractRepositoryTest {
    private static final String USERNAME = "test-name";
    private static final String PASSWORD = "password";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Should find user when exists")
    void shouldFindUser() {
        // given
        User user = User.builder()
                .username(USERNAME)
                .password(PASSWORD)
                .build();
        entityManager.persistAndFlush(user);
        entityManager.clear();

        // when
        Optional<User> result = userRepository.findByUsername(user.getUsername());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo(user.getUsername());
    }

    @Test
    @DisplayName("Should return emply when not found")
    void shouldReturnEmpty_whenNotFound() {
        // when
        Optional<User> result = userRepository.findByUsername("not exists");

        // then
        assertThat(result).isEmpty();
    }
}
