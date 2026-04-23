package com.waynehays.cloudfilestorage.auth.controller;

import com.waynehays.cloudfilestorage.integration.AbstractControllerTest;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends AbstractControllerTest {
    private static final String ANOTHER_USER = "another_user";
    private static final String ANOTHER_PASSWORD = "another_password";

    private ResultActions signUp(String body) throws Exception {
        return mockMvc.perform(post(PATH_SIGN_UP)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions signIn(String body) throws Exception {
        return mockMvc.perform(post(PATH_SIGN_IN)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions signOut(Cookie session) throws Exception {
        return mockMvc.perform(post(PATH_SIGN_OUT)
                .with(csrf())
                .cookie(session));
    }

    private ResultActions getResource(Cookie session) throws Exception {
        return mockMvc.perform(get(PATH_RESOURCE)
                .with(csrf())
                .param(PARAM_PATH, "docs/file.txt")
                .cookie(session));
    }

    @Nested
    class SignUpTests {

        @Test
        @DisplayName("Should return 409 when username already exists")
        void shouldReturn409_whenUsernameAlreadyExists() throws Exception {
            String duplicate = buildRequestBody(DEFAULT_USER, DEFAULT_PASSWORD);

            signUp(duplicate)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", containsString("Username already taken")));
        }

        @Test
        @DisplayName("Should return 400 when username is empty")
        void shouldReturn400_whenUsernameIsEmpty() throws Exception {
            String requestBody = buildRequestBody("", "password");
            signUp(requestBody).andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when username is empty")
        void shouldReturn400_whenPasswordIsEmpty() throws Exception {
            String requestBody = buildRequestBody("user", "");
            signUp(requestBody).andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when body is null")
        void shouldReturn400_whenBodyIsNull() throws Exception {
            mockMvc.perform(post(PATH_SIGN_UP)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should access protected endpoint after sign-up")
        void shouldAccessProtectedEndpoint_afterSignUp() throws Exception {
            getResource(sessionCookie)
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class SignInTests {

        @Test
        @DisplayName("Should return 200 when user exists")
        void shouldReturn200_whenUserExists() throws Exception {
            String signInBody = buildRequestBody(DEFAULT_USER, DEFAULT_PASSWORD);
            signIn(signInBody)
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 401 when wrong password")
        void shouldReturn401_whenWrongPassword() throws Exception {
            String signInBody = buildRequestBody(DEFAULT_USER, "wrong_password");
            signIn(signInBody)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid credentials"));
        }

        @Test
        @DisplayName("Should return 401 when user not exists")
        void shouldReturn401_whenUserNotExists() throws Exception {
            String signInBody = buildRequestBody(ANOTHER_USER, ANOTHER_PASSWORD);
            signIn(signInBody)
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 400 when username is empty")
        void shouldReturn400_whenUsernameIsEmpty() throws Exception {
            String signInBody = buildRequestBody("", "password");
            signIn(signInBody)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("username")));
        }

        @Test
        @DisplayName("Should return 400 when password is empty")
        void shouldReturn400_whenPasswordIsEmpty() throws Exception {
            String signInBody = buildRequestBody("user", "");
            signIn(signInBody)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("password")));
        }

        @Test
        @DisplayName("Should access protected endpoint after sign-in")
        void shouldAccessProtectedEndpoint_afterSignIn() throws Exception {
            getResource(sessionCookie)
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class SignOutTests {

        @Test
        @DisplayName("Should return 204 when authorized user signs out")
        void shouldReturn204_whenAuthorizedUserSignsOut() throws Exception {
            signOut(sessionCookie).andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 401 when unauthorized user access to protected endpoint")
        void shouldReturn401_whenUnauthorizedUserAccessToProtectedEndpoint() throws Exception {
            signOut(sessionCookie).andExpect(status().isNoContent());

            mockMvc.perform(get(PATH_RESOURCE)
                            .with(csrf())
                            .param(PARAM_PATH, "docs/file.txt"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
