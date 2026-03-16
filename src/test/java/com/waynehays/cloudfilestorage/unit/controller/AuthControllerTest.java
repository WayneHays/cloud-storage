package com.waynehays.cloudfilestorage.unit.controller;

import com.waynehays.cloudfilestorage.security.SecurityConfig;
import com.waynehays.cloudfilestorage.controller.AuthController;
import com.waynehays.cloudfilestorage.dto.request.auth.SignInRequest;
import com.waynehays.cloudfilestorage.dto.request.auth.SignUpRequest;
import com.waynehays.cloudfilestorage.dto.response.UserDto;
import com.waynehays.cloudfilestorage.exception.UserAlreadyExistsException;
import com.waynehays.cloudfilestorage.service.user.UserServiceApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {
    private static final String PATH_SIGN_UP = "/api/auth/sign-up";
    private static final String PATH_SIGN_IN = "/api/auth/sign-in";
    private static final String PATH_SIGN_OUT = "/api/auth/sign-out";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserServiceApi userService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("Sign up tests")
    class SignUpTests {

        @Test
        @DisplayName("Should return 201 if valid request")
        void shouldRegisterUser_whenValidData() throws Exception {
            // given
            String name = "name";
            String password = "password";
            SignUpRequest request = new SignUpRequest(name, password);
            UserDto response = new UserDto(name);
            Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(name, null, List.of());

            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            when(userService.signUp(any(SignUpRequest.class))).thenReturn(response);

            // when & then
            mockMvc.perform(post(PATH_SIGN_UP)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username").value(name));
        }

        @Test
        @DisplayName("Should return 400 if invalid request")
        void shouldReturnBadRequest_whenInvalidRequest() throws Exception {
            // given
            SignUpRequest request = new SignUpRequest("user", "pas");

            // when & then
            mockMvc.perform(post(PATH_SIGN_UP)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 409 if duplicate username")
        void shouldReturnConflict_whenDuplicateUsername() throws Exception {
            // given
            SignUpRequest request = new SignUpRequest("name", "password");

            when(userService.signUp(any(SignUpRequest.class)))
                    .thenThrow(new UserAlreadyExistsException("Username already taken", null));

            // when & then
            mockMvc.perform(post(PATH_SIGN_UP)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.messages[0]").value("Username already taken"));
        }
    }

    @Nested
    @DisplayName("Sign in tests")
    class SignInTests {

        @Test
        @DisplayName("Should return 200 if valid credentials")
        void shouldAuthenticateUser_IfValidRequest() throws Exception {
            // given
            String name = "name";
            String password = "password";
            SignInRequest request = new SignInRequest(name, password);
            Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(name, null, List.of());
            when(authenticationManager.authenticate(any())).thenReturn(authentication);

            // when & then
            mockMvc.perform(post(PATH_SIGN_IN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(name));
        }

        @Test
        @DisplayName("Should return 400 if invalid credentials")
        void shouldReturn400_ifInvalidCredentials() throws Exception {
            // given
            SignInRequest request = new SignInRequest("12", "");

            // when & then
            mockMvc.perform(post(PATH_SIGN_IN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 if user unauthorised")
        void shouldReturn401_ifUserUnauthorised() throws Exception {
            // given
            SignInRequest request = new SignInRequest("name", "password");
            when(authenticationManager.authenticate(any()))
                    .thenThrow(BadCredentialsException.class);

            // when & then
            mockMvc.perform(post(PATH_SIGN_IN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(request)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 401 if not existing query")
        void shouldReturn401whenUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/sjhsgh"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Sign-out tests")
    class SignOutTests {

        @Test
        @DisplayName("Should return 204 if succeed")
        void shouldReturn204_ifSucceed() throws Exception {
            mockMvc.perform(post(PATH_SIGN_OUT))
                    .andDo(print())
                    .andExpect(status().isNoContent());
        }
    }
}
