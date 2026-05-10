package com.waynehays.cloudfilestorage.core.user.api;

import com.waynehays.cloudfilestorage.core.user.api.dto.request.SignInRequest;
import com.waynehays.cloudfilestorage.core.user.api.dto.request.SignUpRequest;
import com.waynehays.cloudfilestorage.core.user.api.dto.response.UserResponse;
import com.waynehays.cloudfilestorage.core.user.mapper.UserMapper;
import com.waynehays.cloudfilestorage.core.user.service.UserServiceApi;
import com.waynehays.cloudfilestorage.infrastructure.security.CustomUserDetails;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
class AuthController implements AuthControllerApi {
    private final UserMapper userMapper;
    private final UserServiceApi userService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    @Override
    @PostMapping("/sign-up")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signUp(@RequestBody @Valid SignUpRequest signUpRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) {
        UserResponse registeredUser = userService.signUp(signUpRequest);
        processLogin(signUpRequest.username(), signUpRequest.password(), request, response);
        return registeredUser;
    }

    @Override
    @PostMapping("/sign-in")
    public UserResponse signIn(@RequestBody @Valid SignInRequest signInRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) {
        Authentication authentication = processLogin(signInRequest.username(), signInRequest.password(), request, response);
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userMapper.toResponse(userDetails);
    }

    private Authentication processLogin(String username, String password,
                                        HttpServletRequest request, HttpServletResponse response) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);
        Authentication authentication = authenticationManager.authenticate(token);

        SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            log.info("User signed in: userId={}, username={}", userDetails.id(), username);
        }
        return authentication;
    }
}
