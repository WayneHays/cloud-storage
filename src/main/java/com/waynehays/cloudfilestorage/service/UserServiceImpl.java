package com.waynehays.cloudfilestorage.service;

import com.waynehays.cloudfilestorage.dto.auth.request.SignUpRequest;
import com.waynehays.cloudfilestorage.dto.auth.response.UserDto;
import com.waynehays.cloudfilestorage.entity.User;
import com.waynehays.cloudfilestorage.exception.UserAlreadyExistsException;
import com.waynehays.cloudfilestorage.mapper.UserMapper;
import com.waynehays.cloudfilestorage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    public UserDto signUp(SignUpRequest dto) {
        User user = User.builder()
                .username(dto.username())
                .password(passwordEncoder.encode(dto.password()))
                .build();

        try {
            User saved = userRepository.save(user);
            return userMapper.toDto(saved);
        } catch (DataAccessException e) {
            if (e.getMessage().contains("unique") || e.getMessage().contains("duplicate")) {
                log.info("Attempt to register existing user with username: {}", dto.username());
                throw new UserAlreadyExistsException("Username already taken: " + dto.username(), e);
            }
            throw e;
        }
    }
}
