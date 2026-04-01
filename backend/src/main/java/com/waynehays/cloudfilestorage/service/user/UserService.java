package com.waynehays.cloudfilestorage.service.user;

import com.waynehays.cloudfilestorage.config.properties.UserStorageProperties;
import com.waynehays.cloudfilestorage.dto.request.auth.SignUpRequest;
import com.waynehays.cloudfilestorage.dto.response.UserDto;
import com.waynehays.cloudfilestorage.entity.User;
import com.waynehays.cloudfilestorage.event.UserRegisteredEvent;
import com.waynehays.cloudfilestorage.exception.UserAlreadyExistsException;
import com.waynehays.cloudfilestorage.mapper.UserMapper;
import com.waynehays.cloudfilestorage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService implements UserServiceApi {
    private final UserMapper mapper;
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final UserStorageProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public UserDto signUp(SignUpRequest signUpRequest) {
        User user = mapper.toEntity(signUpRequest);
        user.setPassword(passwordEncoder.encode(signUpRequest.password()));
        user.setStorageLimit(properties.defaultLimit().toBytes());

        try {
            User saved = repository.save(user);
            eventPublisher.publishEvent(new UserRegisteredEvent(saved.getId()));
            return mapper.toDto(saved);
        } catch (DataIntegrityViolationException e) {
            throw new UserAlreadyExistsException("Username already taken: " + signUpRequest.username(), e);
        }
    }

    @Override
    public Page<UserDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }
}
