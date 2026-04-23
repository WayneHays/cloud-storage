package com.waynehays.cloudfilestorage.auth.service;

import com.waynehays.cloudfilestorage.auth.dto.request.SignUpRequest;
import com.waynehays.cloudfilestorage.auth.dto.response.UserDto;
import com.waynehays.cloudfilestorage.auth.entity.User;
import com.waynehays.cloudfilestorage.auth.event.UserRegisteredEvent;
import com.waynehays.cloudfilestorage.shared.exception.UserAlreadyExistsException;
import com.waynehays.cloudfilestorage.auth.mapper.UserMapper;
import com.waynehays.cloudfilestorage.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService implements UserServiceApi {
    private final UserMapper mapper;
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public UserDto signUp(SignUpRequest signUpRequest) {
        User user = mapper.toEntity(signUpRequest);
        user.setPassword(passwordEncoder.encode(signUpRequest.password()));

        try {
            User saved = repository.saveAndFlush(user);
            eventPublisher.publishEvent(new UserRegisteredEvent(saved.getId()));
            return mapper.toDto(saved);
        } catch (DataIntegrityViolationException e) {
            throw new UserAlreadyExistsException("Username already taken: " + signUpRequest.username(), e);
        }
    }
}
