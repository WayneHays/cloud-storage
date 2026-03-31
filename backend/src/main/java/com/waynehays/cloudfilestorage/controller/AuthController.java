package com.waynehays.cloudfilestorage.controller;

import com.waynehays.cloudfilestorage.dto.request.auth.SignInRequest;
import com.waynehays.cloudfilestorage.dto.request.auth.SignUpRequest;
import com.waynehays.cloudfilestorage.dto.response.UserDto;
import com.waynehays.cloudfilestorage.mapper.UserMapper;
import com.waynehays.cloudfilestorage.service.user.UserServiceApi;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserMapper userMapper;
    private final UserServiceApi userService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    @PostMapping("/sign-up")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto signUp(@RequestBody @Valid SignUpRequest signUpRequest,
                                          HttpServletRequest request,
                                          HttpServletResponse response) {
        UserDto registeredUser = userService.signUp(signUpRequest);
        log.info("User registered: {}", signUpRequest.username());

        processLogin(signUpRequest.username(), signUpRequest.password(), request, response);
        return registeredUser;
    }

    @PostMapping("/sign-in")
    public UserDto signIn(@RequestBody @Valid SignInRequest signInRequest,
                                          HttpServletRequest request,
                                          HttpServletResponse response) {
        processLogin(signInRequest.username(), signInRequest.password(), request, response);
        return userMapper.toDto(signInRequest);
    }

    private void processLogin(String username, String password, HttpServletRequest request, HttpServletResponse response) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);
        Authentication authentication = authenticationManager.authenticate(token);

        SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        log.info("User signed in: {}", username);
    }
}
