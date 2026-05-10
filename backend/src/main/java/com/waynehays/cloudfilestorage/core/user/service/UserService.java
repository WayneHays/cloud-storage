package com.waynehays.cloudfilestorage.core.user.service;

import com.waynehays.cloudfilestorage.core.user.api.dto.request.SignUpRequest;
import com.waynehays.cloudfilestorage.core.user.api.dto.response.UserResponse;
import com.waynehays.cloudfilestorage.core.user.entity.User;
import com.waynehays.cloudfilestorage.core.user.event.UserRegisteredEvent;
import com.waynehays.cloudfilestorage.core.user.exception.UserAlreadyExistsException;
import com.waynehays.cloudfilestorage.core.user.factory.UserFactory;
import com.waynehays.cloudfilestorage.core.user.mapper.UserMapper;
import com.waynehays.cloudfilestorage.core.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class UserService implements UserServiceApi {
    private final UserMapper mapper;
    private final UserFactory factory;
    private final UserRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public UserResponse signUp(SignUpRequest signUpRequest) {
        User user = factory.create(signUpRequest);
        try {
            User saved = repository.saveAndFlush(user);
            log.info("User registered: userId={}, username={}", saved.getId(), saved.getUsername());
            eventPublisher.publishEvent(new UserRegisteredEvent(saved.getId()));
            return mapper.toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new UserAlreadyExistsException("Username already taken: " + signUpRequest.username(), e);
        }
    }
}
