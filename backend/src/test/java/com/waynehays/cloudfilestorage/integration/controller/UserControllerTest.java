package com.waynehays.cloudfilestorage.integration.controller;

import com.waynehays.cloudfilestorage.integration.base.AbstractRestControllerBaseTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest extends AbstractRestControllerBaseTest {
    private static final String PATH_ME = "/api/user/me";
    private static final String PATH_SIGN_OUT = "/api/auth/sign-out";

    @Test
    @DisplayName("Should return user info when authenticated")
    void shouldReturnUserInfo_whenAuthenticated() throws Exception {
        Cookie sessionCookie = registerAndLoginDefaultUser();

        mockMvc.perform(get(PATH_ME)
                        .with(csrf())
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("user"));
    }

    @Test
    @DisplayName("Should return 401 when no session cookie provided")
    void shouldReturn401_whenNoSessionCookie() throws Exception {
        mockMvc.perform(get(PATH_ME)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 after user logs out")
    void shouldReturn401_afterUserLogsOut() throws Exception {
        Cookie sessionCookie = registerAndLoginDefaultUser();

        mockMvc.perform(post(PATH_SIGN_OUT)
                        .with(csrf())
                        .cookie(sessionCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(PATH_ME)
                        .with(csrf())
                        .cookie(sessionCookie))
                .andExpect(status().isUnauthorized());
    }
}
