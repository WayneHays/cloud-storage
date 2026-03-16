package com.waynehays.cloudfilestorage.unit.service.user;

import com.waynehays.cloudfilestorage.dto.request.auth.SignUpRequest;
import com.waynehays.cloudfilestorage.dto.response.UserDto;
import com.waynehays.cloudfilestorage.entity.User;
import com.waynehays.cloudfilestorage.exception.UserAlreadyExistsException;
import com.waynehays.cloudfilestorage.mapper.UserMapper;
import com.waynehays.cloudfilestorage.repository.UserRepository;
import com.waynehays.cloudfilestorage.service.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String HASHED_PASSWORD = "hashedPassword";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Should successfully register user")
    void shouldSuccessfullyRegisterUser() {
        // given
        SignUpRequest request = new SignUpRequest(USERNAME, PASSWORD);
        UserDto expectedDto = new UserDto(USERNAME);

        doReturn(HASHED_PASSWORD).when(passwordEncoder).encode(PASSWORD);
        when(userRepository.save(Mockito.any())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(userMapper.toDto(Mockito.any())).thenReturn(expectedDto);

        // when
        UserDto result = userService.signUp(request);

        // then
        assertThat(result).isEqualTo(expectedDto);
        verify(passwordEncoder).encode(PASSWORD);
    }

    @Test
    @DisplayName("Should encode password before saving")
    void shouldEncodePassword() {
        // given
        SignUpRequest request = new SignUpRequest(USERNAME, PASSWORD);

        doReturn(HASHED_PASSWORD).when(passwordEncoder).encode(PASSWORD);
        when(userRepository.save(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toDto(Mockito.any())).thenReturn(new UserDto(USERNAME));

        // when
        userService.signUp(request);

        // then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo(HASHED_PASSWORD);
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when unique constraint violated")
    void shouldThrowWhenDuplicateUnique() {
        // given
        SignUpRequest request = new SignUpRequest(USERNAME, PASSWORD);

        doReturn(HASHED_PASSWORD).when(passwordEncoder).encode(PASSWORD);
        when(userRepository.save(Mockito.any()))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        // when & then
        assertThatThrownBy(() -> userService.signUp(request))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when duplicate key detected")
    void shouldThrowWhenDuplicateKey() {
        // given
        SignUpRequest request = new SignUpRequest(USERNAME, PASSWORD);

        doReturn(HASHED_PASSWORD).when(passwordEncoder).encode(PASSWORD);
        when(userRepository.save(Mockito.any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        // when & then
        assertThatThrownBy(() -> userService.signUp(request))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    @DisplayName("Should rethrow DataAccessException when not a duplicate error")
    void shouldRethrowNonDuplicateException() {
        // given
        SignUpRequest request = new SignUpRequest(USERNAME, PASSWORD);

        doReturn(HASHED_PASSWORD).when(passwordEncoder).encode(PASSWORD);
        when(userRepository.save(Mockito.any()))
                .thenThrow(new DataIntegrityViolationException("connection timeout"));

        // when & then
        assertThatThrownBy(() -> userService.signUp(request))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("connection timeout");
    }

}
