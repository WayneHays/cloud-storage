package com.waynehays.cloudfilestorage.unit.mapper;

import com.waynehays.cloudfilestorage.dto.response.UserDto;
import com.waynehays.cloudfilestorage.entity.User;
import com.waynehays.cloudfilestorage.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {
    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    @DisplayName("Should map User to UserDto with username")
    void shouldMapUserToDto() {
        // given
        User user = User.builder()
                .id(1L)
                .username("john_doe")
                .password("encoded-password-123")
                .build();

        // when
        UserDto result = mapper.toDto(user);

        // then
        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("john_doe");
    }

    @Test
    @DisplayName("Should handle user with special characters in username")
    void shouldHandleSpecialCharactersInUsername() {
        // given
        User user = User.builder()
                .id(2L)
                .username("user.name+tag@domain.com")
                .password("another-encoded-pass")
                .build();

        // when
        UserDto result = mapper.toDto(user);

        // then
        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("user.name+tag@domain.com");
    }

    @Test
    @DisplayName("Should handle user with empty username gracefully")
    void shouldHandleEmptyUsername() {
        // given
        User user = User.builder()
                .id(3L)
                .username("")
                .password("password")
                .build();

        // when
        UserDto result = mapper.toDto(user);

        // then
        assertThat(result).isNotNull();
        assertThat(result.username()).isEmpty();
    }

    @Test
    @DisplayName("Should handle null user gracefully")
    void shouldHandleNullUser() {
        // given — null user

        // when
        UserDto result = mapper.toDto(null);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should preserve username case sensitivity")
    void shouldPreserveUsernameCaseSensitivity() {
        // given
        User user = User.builder()
                .id(4L)
                .username("John_Doe_2024")
                .password("pass123")
                .build();

        // when
        UserDto result = mapper.toDto(user);

        // then
        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("John_Doe_2024");
    }

    @Test
    @DisplayName("Should map multiple users to list of UserDto")
    void shouldMapListOfUsers() {
        // given
        User user1 = User.builder()
                .id(5L)
                .username("alice")
                .password("pass1")
                .build();

        User user2 = User.builder()
                .id(6L)
                .username("bob")
                .password("pass2")
                .build();

        List<User> users = List.of(user1, user2);

        // when
        List<UserDto> result = users.stream()
                .map(mapper::toDto)
                .toList();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).username()).isEqualTo("alice");
        assertThat(result.get(1).username()).isEqualTo("bob");
    }

    @Test
    @DisplayName("Should handle user with whitespace in username")
    void shouldHandleWhitespaceInUsername() {
        // given
        User user = User.builder()
                .id(7L)
                .username("  trimmed_username  ")
                .password("password")
                .build();

        // when
        UserDto result = mapper.toDto(user);

        // then
        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("  trimmed_username  ");
    }
}
