package com.waynehays.cloudfilestorage.core.user;

import com.waynehays.cloudfilestorage.core.user.dto.request.SignUpRequest;
import com.waynehays.cloudfilestorage.core.user.dto.response.UserDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    @DisplayName("Should map id and username from entity")
    void shouldMapIdAndUsernameFromEntity() {
        // given
        User user = new User();
        user.setId(1L);
        user.setUsername("john_doe");

        // when
        UserDto result = mapper.toDto(user);

        // then
        assertThat(result.username()).isEqualTo("john_doe");
    }

    @Test
    @DisplayName("Should map id and username from CustomUserDetails")
    void shouldMapIdAndUsernameFromCustomUserDetails() {
        // given
        CustomUserDetails userDetails = new CustomUserDetails(42L, "jane_doe", "secret");

        // when
         UserDto result = mapper.toDto(userDetails);

        // then
        assertThat(result.username()).isEqualTo("jane_doe");
    }

    @Test
    @DisplayName("Should map username from SignUpRequest and ignore other fields")
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
