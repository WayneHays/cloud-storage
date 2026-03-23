package com.waynehays.cloudfilestorage.security;

import com.waynehays.cloudfilestorage.entity.User;
import com.waynehays.cloudfilestorage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private static final String MSG_NOT_FOUND = "User with username %s not found";

    private final UserRepository userRepository;

    @NotNull
    @Override
    public UserDetails loadUserByUsername(@NotNull String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new UsernameNotFoundException(MSG_NOT_FOUND.formatted(username)));
        return new CustomUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword()
        );
    }
}
