package com.waynehays.cloudfilestorage.unit.service.user;

import com.waynehays.cloudfilestorage.config.properties.UserStorageLimitProperties;
import com.waynehays.cloudfilestorage.dto.request.auth.SignUpRequest;
import com.waynehays.cloudfilestorage.dto.response.UserDto;
import com.waynehays.cloudfilestorage.entity.User;
import com.waynehays.cloudfilestorage.event.UserRegisteredEvent;
import com.waynehays.cloudfilestorage.exception.UserAlreadyExistsException;
import com.waynehays.cloudfilestorage.mapper.UserMapper;
import com.waynehays.cloudfilestorage.repository.UserRepository;
import com.waynehays.cloudfilestorage.service.user.UserService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserStorageLimitProperties properties;

    @InjectMocks
    private UserService userService;

    @Nested
    class SignUp {

        @Test
        void shouldRegisterUserAndPublishEvent() {
            // given
            SignUpRequest request = new SignUpRequest("testuser", "password123");
            User mappedUser = new User();
            mappedUser.setUsername("testuser");

            User savedUser = new User();
            savedUser.setId(1L);
            savedUser.setUsername("testuser");
            savedUser.setPassword("encoded_password");

            UserDto expectedDto = new UserDto("testuser");

            when(userMapper.toEntity(request)).thenReturn(mappedUser);
            when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
            when(properties.defaultLimit().toBytes()).thenReturn(100L);
            when(userRepository.save(mappedUser)).thenReturn(savedUser);
            when(userMapper.toDto(savedUser)).thenReturn(expectedDto);

            // when
            UserDto result = userService.signUp(request);

            // then
            assertThat(result.username()).isEqualTo("testuser");
            verify(passwordEncoder).encode("password123");
            verify(userRepository).save(mappedUser);
            verify(eventPublisher).publishEvent(new UserRegisteredEvent(1L));
        }

        @Test
        void shouldSetEncodedPassword() {
            // given
            SignUpRequest request = new SignUpRequest("testuser", "password123");
            User mappedUser = new User();
            User savedUser = new User();
            savedUser.setId(1L);

            when(userMapper.toEntity(request)).thenReturn(mappedUser);
            when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
            when(properties.defaultLimit().toBytes()).thenReturn(100L);
            when(userRepository.save(mappedUser)).thenReturn(savedUser);
            when(userMapper.toDto(savedUser)).thenReturn(new UserDto("testuser"));

            // when
            userService.signUp(request);

            // then
            assertThat(mappedUser.getPassword()).isEqualTo("encoded_password");
            assertThat(mappedUser.getStorageLimit()).isEqualTo(100L);
        }

        @Test
        void shouldThrowWhenUsernameAlreadyTaken() {
            // given
            SignUpRequest request = new SignUpRequest("existinguser", "password123");
            User mappedUser = new User();

            when(userMapper.toEntity(request)).thenReturn(mappedUser);
            when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
            when(properties.defaultLimit().toBytes()).thenReturn(100L);
            when(userRepository.save(mappedUser))
                    .thenThrow(new DataIntegrityViolationException("unique constraint"));

            // when & then
            assertThatThrownBy(() -> userService.signUp(request))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("existinguser");

            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    class GetUserStorageLimit {

        @Test
        void shouldReturnStorageLimit() {
            // given
            when(userRepository.getStorageLimitById(1L)).thenReturn(100L);

            // when
            Long result = userService.getUserStorageLimit(1L);

            // then
            assertThat(result).isEqualTo(100L);
        }
    }
}
