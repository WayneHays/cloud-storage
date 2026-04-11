package com.waynehays.cloudfilestorage.controller.user;

import com.waynehays.cloudfilestorage.dto.response.UserDto;
import com.waynehays.cloudfilestorage.mapper.UserMapper;
import com.waynehays.cloudfilestorage.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController implements UserControllerApi{
    private final UserMapper mapper;

    @Override
    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return mapper.toDto(userDetails);
    }
}
