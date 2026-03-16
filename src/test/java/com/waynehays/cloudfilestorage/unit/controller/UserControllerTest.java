package com.waynehays.cloudfilestorage.unit.controller;

import com.waynehays.cloudfilestorage.security.SecurityConfig;
import com.waynehays.cloudfilestorage.controller.UserController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {
    private static final String PATH_ME = "/api/user/me";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should return 200 with authorised user")
    @WithMockUser(username = "name")
    void shouldReturn200_ifAuthorisedUser() throws Exception {
        mockMvc.perform(get(PATH_ME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("name"));
    }

    @Test
    @DisplayName("Should return 401 with anonymous user")
    @WithAnonymousUser
    void shouldReturn401_ifAnonymousUser() throws Exception {
        mockMvc.perform(get(PATH_ME))
                .andExpect(status().isUnauthorized());
    }
}
