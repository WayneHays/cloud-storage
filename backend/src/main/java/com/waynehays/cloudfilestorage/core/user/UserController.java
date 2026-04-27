package com.waynehays.cloudfilestorage.core.user;

import com.waynehays.cloudfilestorage.core.user.dto.response.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
class UserController implements UserControllerApi{
    private final UserMapper mapper;

    @Override
    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return mapper.toDto(userDetails);
    }
}
