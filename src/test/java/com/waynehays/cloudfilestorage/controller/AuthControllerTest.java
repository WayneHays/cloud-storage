package com.waynehays.cloudfilestorage.controller;

import com.waynehays.cloudfilestorage.config.SecurityConfig;
import com.waynehays.cloudfilestorage.dto.request.SignUpRequest;
import com.waynehays.cloudfilestorage.dto.response.UserDto;
import com.waynehays.cloudfilestorage.exception.UserAlreadyExistsException;
import com.waynehays.cloudfilestorage.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {
    private static final String URL_REGISTER = "/api/auth/sign-up";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should register user when valid request")
    void shouldRegisterUser_whenValidData() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest("name", "password");
        UserDto response = new UserDto("name");
        when(userService.signUp(any(SignUpRequest.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post(URL_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("name"));
    }

    @Test
    @DisplayName("Should return 409 when duplicate username")
    void shouldReturnConflict_whenDuplicateUsername() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest("name", "password");

        when(userService.signUp(any(SignUpRequest.class)))
                .thenThrow(new UserAlreadyExistsException("Username already taken", null));

        // when & then
        mockMvc.perform(post(URL_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already taken"));
    }

    @Test
    @DisplayName("Should return 400 when invalid data")
    void shouldReturnBadRequest_whenInvalidData() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest("ab", "123");

        // when & then
        mockMvc.perform(post(URL_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @DisplayName("Should return 401 if unauthenticated")
    void shouldReturn401whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/sjhsgh"))
                .andExpect(status().isUnauthorized());
    }
}
