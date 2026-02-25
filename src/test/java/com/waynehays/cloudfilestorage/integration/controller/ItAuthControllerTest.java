package com.waynehays.cloudfilestorage.integration.controller;

import com.waynehays.cloudfilestorage.dto.auth.request.SignInRequest;
import com.waynehays.cloudfilestorage.dto.auth.request.SignUpRequest;
import com.waynehays.cloudfilestorage.integration.base.AbstractControllerItTest;
import com.waynehays.cloudfilestorage.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ItAuthControllerTest extends AbstractControllerItTest {
    private static final String SIGN_UP_URL = "/api/auth/sign-up";
    private static final String SIGN_IN_URL = "/api/auth/sign-in";
    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should register new user and return 201")
    void shouldRegisterUser() throws Exception {
        // given
        SignUpRequest signUpRequest = new SignUpRequest(USERNAME, PASSWORD);

        // when
        mockMvc.perform(post(SIGN_UP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(USERNAME));

        // then
        assertThat(userRepository.findByUsername(USERNAME)).isPresent();
    }

    @Test
    @DisplayName("Should return 409 when duplicate username")
    void shouldReturn409_whenDuplicateUsername() throws Exception {
        // given
        SignUpRequest signUpRequest = new SignUpRequest(USERNAME, PASSWORD);

        // when & then
        mockMvc.perform(post(SIGN_UP_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequest)));
        mockMvc.perform(post(SIGN_UP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andDo(print())
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should return 400 when username is blank")
    void shouldReturn400_whenUsernameIsBlank() throws Exception {
        // given
        SignUpRequest signUpRequest = new SignUpRequest("", PASSWORD);

        // when & then
        mockMvc.perform(post(SIGN_UP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should sign in existing user and return 200")
    void shouldSignInUser() throws Exception {
        // given
        SignUpRequest signUpRequest = new SignUpRequest(USERNAME, PASSWORD);
        mockMvc.perform(post(SIGN_UP_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequest)));

        // when & then
        SignInRequest signInRequest = new SignInRequest(USERNAME, PASSWORD);
        mockMvc.perform(post(SIGN_IN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signInRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USERNAME));
    }

    @Test
    @DisplayName("Should return 401 when wrong password")
    void shouldReturn401_whenWrongPassword() throws Exception {
        // given
        SignUpRequest signUpRequest = new SignUpRequest(USERNAME, PASSWORD);
        mockMvc.perform(post(SIGN_UP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andDo(print());

        // when & then
        SignInRequest signInRequest = new SignInRequest(USERNAME, "wrongpassword");
        mockMvc.perform(post(SIGN_IN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signInRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when user not found")
    void shouldReturn401_WhenUserNotFound() throws Exception {
        // given
        SignInRequest signInRequest = new SignInRequest("nonexistent", PASSWORD);

        // when & then
        mockMvc.perform(post(SIGN_IN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signInRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
}
