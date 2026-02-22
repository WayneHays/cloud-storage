package com.waynehays.cloudfilestorage.unit.service;

import com.waynehays.cloudfilestorage.dto.auth.request.SignUpRequest;
import com.waynehays.cloudfilestorage.dto.auth.response.UserDto;
import com.waynehays.cloudfilestorage.entity.User;
import com.waynehays.cloudfilestorage.exception.UserAlreadyExistsException;
import com.waynehays.cloudfilestorage.mapper.UserMapper;
import com.waynehays.cloudfilestorage.repository.UserRepository;
import com.waynehays.cloudfilestorage.service.UserServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl service;

    private SignUpRequest signUpRequest = new SignUpRequest("username", "password");

    @Test
    @DisplayName("Should successfully register new user")
    void givenUsernameAndPassword_whenRegister_shouldReturnRegisteredUser() {
        // given
        User user = new User(1L, "name", "password");
        UserDto userDto = new UserDto("name");
        when(passwordEncoder.encode(signUpRequest.password())).thenReturn("hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        // when
        UserDto result = service.signUp(signUpRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("name");
        verify(passwordEncoder).encode(any(String.class));
        verify(userRepository).save(any(User.class));
        verify(userMapper).toDto(any(User.class));
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when duplicate name")
    void givenUsernameAndPassword_whenRegisterDuplicateName_shouldThrowException() {
        // given
        when(passwordEncoder.encode(any(String.class)))
                .thenReturn("hashedpassword");
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        // when & then
        assertThatThrownBy(() -> service.signUp(signUpRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("Username already taken");
    }
}
