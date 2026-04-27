package com.waynehays.cloudfilestorage.core.user;

import com.waynehays.cloudfilestorage.core.user.dto.request.SignUpRequest;
import com.waynehays.cloudfilestorage.core.user.dto.response.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class UserService implements UserServiceApi {
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
            log.info("User registered: userId={}, username={}", user.getId(), user.getUsername());
            return mapper.toDto(saved);
        } catch (DataIntegrityViolationException e) {
            throw new UserAlreadyExistsException("Username already taken: " + signUpRequest.username(), e);
        }
    }
}
