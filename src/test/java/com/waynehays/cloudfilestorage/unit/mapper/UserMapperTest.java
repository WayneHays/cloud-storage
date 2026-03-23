package com.waynehays.cloudfilestorage.unit.mapper;

import com.waynehays.cloudfilestorage.dto.request.auth.SignUpRequest;
import com.waynehays.cloudfilestorage.dto.response.UserDto;
import com.waynehays.cloudfilestorage.entity.User;
import com.waynehays.cloudfilestorage.mapper.UserMapper;
import com.waynehays.cloudfilestorage.mapper.UserMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapperImpl();
    }

    @Nested
    class ToDto {

        @Test
        void shouldMapUserToDto() {
            // given
            User user = new User();
            user.setId(1L);
            user.setUsername("testuser");
            user.setPassword("encoded_password");

            // when
            UserDto result = userMapper.toDto(user);

            // then
            assertThat(result.username()).isEqualTo("testuser");
        }
    }

    @Nested
    class ToEntity {

        @Test
        void shouldMapSignUpRequestToUser() {
            // given
            SignUpRequest request = new SignUpRequest("testuser", "password123");

            // when
            User result = userMapper.toEntity(request);

            // then
            assertThat(result.getUsername()).isEqualTo("testuser");
            assertThat(result.getId()).isNull();
            assertThat(result.getPassword()).isNull();
        }
    }
}
