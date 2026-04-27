package com.waynehays.cloudfilestorage.core.user;

import com.waynehays.cloudfilestorage.AbstractControllerTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest extends AbstractControllerTest {

    @Test
    @DisplayName("Should return user info when authenticated")
    void shouldReturnUserInfo_whenAuthenticated() throws Exception {
        mockMvc.perform(get(PATH_ME)
                        .with(csrf())
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
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
