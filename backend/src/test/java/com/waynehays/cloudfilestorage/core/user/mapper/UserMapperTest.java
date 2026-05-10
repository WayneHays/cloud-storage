package com.waynehays.cloudfilestorage.core.user.mapper;

import com.waynehays.cloudfilestorage.core.user.api.dto.response.UserResponse;
import com.waynehays.cloudfilestorage.core.user.entity.User;
import com.waynehays.cloudfilestorage.infrastructure.security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    @DisplayName("Should map only username from CustomUserDetails")
    void shouldMapUsernameFromCustomUserDetails() {
        // given
        CustomUserDetails userDetails = new CustomUserDetails(42L, "jane_doe", "secret");

        // when
        UserResponse result = mapper.toResponse(userDetails);

        // then
        assertThat(result.username()).isEqualTo("jane_doe");
    }

    @Test
    @DisplayName("Should map only username from User")
    void shouldMapUsername_fromUser() {
        // given
        User user = new User("username", "password");

        // when
        UserResponse result = mapper.toResponse(user);

        // then
        assertThat(result.username()).isEqualTo("username");
    }
}
