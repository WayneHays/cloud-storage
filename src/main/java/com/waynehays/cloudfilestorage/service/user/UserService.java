package com.waynehays.cloudfilestorage.service.user;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService implements UserServiceApi {
    private static final String MSG_ALREADY_TAKEN = "Username already taken: ";

    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserDto signUp(SignUpRequest signUpRequest) {
        User user = userMapper.toEntity(signUpRequest);
        user.setPassword(passwordEncoder.encode(signUpRequest.password()));

        try {
            User saved = userRepository.save(user);
            eventPublisher.publishEvent(new UserRegisteredEvent(saved.getId()));
            return userMapper.toDto(saved);
        } catch (DataIntegrityViolationException e) {
            throw new UserAlreadyExistsException(MSG_ALREADY_TAKEN + signUpRequest.username(), e);
        }
    }
}
