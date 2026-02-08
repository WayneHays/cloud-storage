package com.waynehays.cloudfilestorage;

import com.waynehays.cloudfilestorage.entity.User;
import com.waynehays.cloudfilestorage.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryTest {
    private static final String USERNAME = "John";
    private static final String PASSWORD = "password";

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username(USERNAME)
                .password(PASSWORD)
                .build();
    }

    @Test
    void givenUser_whenSaveUser_shouldSaveUserAndGenerateId() {
        // when
        User saved = userRepository.save(user);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUsername()).isEqualTo(USERNAME);
    }

    @Test
    void givenUser_whenFindByUsername_shouldReturnUser() {
        // given
        userRepository.save(user);

        // when
        Optional<User> found = userRepository.findByUsername(USERNAME);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo(USERNAME);
    }

    @Test
    void givenUser_whenUserNotFound_shouldReturnEmpty() {
        // when
        Optional<User> found = userRepository.findByUsername("non");

        // then
        assertThat(found).isEmpty();
    }
}
