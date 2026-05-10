package com.waynehays.cloudfilestorage.core.user.service;

import com.waynehays.cloudfilestorage.core.user.api.dto.request.SignUpRequest;
import com.waynehays.cloudfilestorage.core.user.api.dto.response.UserResponse;
import com.waynehays.cloudfilestorage.core.user.entity.User;
import com.waynehays.cloudfilestorage.core.user.event.UserRegisteredEvent;
import com.waynehays.cloudfilestorage.core.user.exception.UserAlreadyExistsException;
import com.waynehays.cloudfilestorage.core.user.factory.UserFactory;
import com.waynehays.cloudfilestorage.core.user.mapper.UserMapper;
import com.waynehays.cloudfilestorage.core.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

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
    private UserFactory factory;

    @Mock
    private UserMapper mapper;

    @Mock
    private UserRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserService service;

    @Test
    @DisplayName("Should create user via factory, save and publish event")
    void signUp_shouldCreateSaveAndPublishEvent() {
        // given
        SignUpRequest request = new SignUpRequest("john_doe", "password123");

        User createdUser = new User("john_doe", "encoded_password");
        User savedUser = new User("john_doe", "encoded_password");
        ReflectionTestUtils.setField(savedUser, "id", 1L);
        UserResponse expectedDto = new UserResponse("john_doe");

        when(factory.create(request)).thenReturn(createdUser);
        when(repository.saveAndFlush(createdUser)).thenReturn(savedUser);
        when(mapper.toResponse(savedUser)).thenReturn(expectedDto);

        // when
        UserResponse result = service.signUp(request);

        // then
        assertThat(result).isEqualTo(expectedDto);

        InOrder inOrder = inOrder(repository, eventPublisher);
        inOrder.verify(repository).saveAndFlush(createdUser);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        inOrder.verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(UserRegisteredEvent.class);
        assertThat(((UserRegisteredEvent) captor.getValue()).userId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when username is taken")
    void signUp_shouldThrowWhenUsernameAlreadyTaken() {
        // given
        SignUpRequest request = new SignUpRequest("existing_user", "password123");
        User createdUser = new User("existing_user", "encoded_password");

        when(factory.create(request)).thenReturn(createdUser);
        when(repository.saveAndFlush(createdUser))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        // when & then
        assertThatThrownBy(() -> service.signUp(request))
                .isInstanceOf(UserAlreadyExistsException.class);
        verify(eventPublisher, never()).publishEvent(any());
    }
}