package com.waynehays.cloudfilestorage.infrastructure.security;

import com.waynehays.cloudfilestorage.core.user.entity.User;
import com.waynehays.cloudfilestorage.core.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl service;

    @Test
    @DisplayName("Should return CustomUserDetails with id, username and password")
    void shouldReturnCustomUserDetails() {
        // given
        User user = new User("john", "encoded");
        ReflectionTestUtils.setField(user, "id", 7L);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        // when
        UserDetails result = service.loadUserByUsername("john");

        // then
        assertThat(result).isInstanceOf(CustomUserDetails.class);
        CustomUserDetails details = (CustomUserDetails) result;
        assertThat(details.id()).isEqualTo(7L);
        assertThat(details.username()).isEqualTo("john");
        assertThat(details.password()).isEqualTo("encoded");
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user not found")
    void shouldThrowWhenUserNotFound() {
        // given
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("missing");
    }
}