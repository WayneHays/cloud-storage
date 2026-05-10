package com.waynehays.cloudfilestorage.core.user.factory;

import com.waynehays.cloudfilestorage.core.user.api.dto.request.SignUpRequest;
import com.waynehays.cloudfilestorage.core.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserFactory {
    private final PasswordEncoder passwordEncoder;

    public User create(SignUpRequest request) {
        return new User(
                request.username(),
                passwordEncoder.encode(request.password())
        );
    }
}
