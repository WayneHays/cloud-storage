package com.waynehays.cloudfilestorage.core.user.factory;

import com.waynehays.cloudfilestorage.core.user.api.dto.request.SignUpRequest;
import com.waynehays.cloudfilestorage.core.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserFactoryTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserFactory factory;

    @Test
    @DisplayName("Should create user with encoded password, not raw")
    void shouldEncodePassword() {
        // given
        SignUpRequest request = new SignUpRequest("john", "raw-password");
        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");

        // when
        User user = factory.create(request);

        // then
        assertThat(user.getUsername()).isEqualTo("john");
        assertThat(user.getPassword())
                .isEqualTo("encoded-password")
                .isNotEqualTo("raw-password");
    }
}