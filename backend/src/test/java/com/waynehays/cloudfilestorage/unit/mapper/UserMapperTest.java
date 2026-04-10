package com.waynehays.cloudfilestorage.unit.mapper;

import com.waynehays.cloudfilestorage.dto.request.auth.SignInRequest;
import com.waynehays.cloudfilestorage.dto.request.auth.SignUpRequest;
import com.waynehays.cloudfilestorage.dto.response.UserDto;
import com.waynehays.cloudfilestorage.entity.User;
import com.waynehays.cloudfilestorage.mapper.UserMapper;
import com.waynehays.cloudfilestorage.mapper.UserMapperImpl;
import com.waynehays.cloudfilestorage.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    void toDtoFromUser_shouldMapIdAndUsername() {
        // given
        User user = new User();
        user.setId(1L);
        user.setUsername("john_doe");

        // when
        UserDto result = mapper.toDto(user);

        // then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.username()).isEqualTo("john_doe");
    }

    @Test
    void toDtoFromUserDetails_shouldMapIdAndUsername() {
        // given
        CustomUserDetails userDetails = new CustomUserDetails(42L, "jane_doe", "secret");

        // when
         UserDto result = mapper.toDto(userDetails);

        // then
        assertThat(result.id()).isEqualTo(42L);
        assertThat(result.username()).isEqualTo("jane_doe");
    }

    @Test
    void toEntity_shouldMapUsernameAndIgnoreOtherFields() {
        // given
        SignUpRequest request = new SignUpRequest("jane_doe", "secret123");

        // when
        User result = mapper.toEntity(request);

        // then
        assertThat(result.getUsername()).isEqualTo("jane_doe");
        assertThat(result.getId()).isNull();
        assertThat(result.getPassword()).isNull();
        assertThat(result.getCreatedAt()).isNull();
        assertThat(result.getUpdatedAt()).isNull();
    }
}
