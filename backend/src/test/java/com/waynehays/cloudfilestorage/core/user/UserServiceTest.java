package com.waynehays.cloudfilestorage.core.user;

import com.waynehays.cloudfilestorage.core.user.api.dto.SignUpRequest;
import com.waynehays.cloudfilestorage.core.user.dto.response.UserDto;
import com.waynehays.cloudfilestorage.core.user.exception.UserAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper mapper;

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserService service;

    @Test
    void signUp_shouldEncodePasswordSaveAndPublishEvent() {
        // given
        SignUpRequest request = new SignUpRequest("john_doe", "password123");
        User mappedUser = new User();
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("john_doe");
        UserDto expectedDto = new UserDto("john_doe");

        when(mapper.toEntity(request)).thenReturn(mappedUser);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(repository.saveAndFlush(mappedUser)).thenReturn(savedUser);
        when(mapper.toDto(savedUser)).thenReturn(expectedDto);

        // when
        UserDto result = service.signUp(request);

        // then
        assertThat(result).isEqualTo(expectedDto);
        assertThat(mappedUser.getPassword()).isEqualTo("encoded_password");

        InOrder inOrder = inOrder(repository, eventPublisher);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        inOrder.verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(UserRegisteredEvent.class);
        assertThat(((UserRegisteredEvent) captor.getValue()).userId()).isEqualTo(1L);
    }

    @Test
    void signUp_shouldThrowWhenUsernameAlreadyTaken() {
        // given
        SignUpRequest request = new SignUpRequest("existing_user", "password123");
        User mappedUser = new User();

        when(mapper.toEntity(request)).thenReturn(mappedUser);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(repository.saveAndFlush(mappedUser))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        // when & then
        assertThatThrownBy(() -> service.signUp(request))
                .isInstanceOf(UserAlreadyExistsException.class);
        verify(eventPublisher, never()).publishEvent(any());
    }
}
