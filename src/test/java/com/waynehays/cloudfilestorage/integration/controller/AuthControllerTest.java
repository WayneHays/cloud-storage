package com.waynehays.cloudfilestorage.integration.controller;

import com.waynehays.cloudfilestorage.integration.base.AbstractRestControllerBaseTest;
import com.waynehays.cloudfilestorage.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends AbstractRestControllerBaseTest {

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Nested
    class SignUpTests {
        private static final String SIGN_UP_PATH = "/api/auth/sign-up";

        @Test
        @DisplayName("Should register new user and return 201")
        void shouldRegisterNewUserAndReturn201() throws Exception {
            registerAndLoginDefaultUser();
        }

        @Test
        @DisplayName("Should return 409 when username already exists")
        void shouldReturn409_whenUsernameAlreadyExists() throws Exception {
            registerAndLoginDefaultUser();

            String duplicate = """
                    {
                        "username": "user",
                        "password": "password"
                    }
                    """;
            mockMvc.perform(post(SIGN_UP_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(duplicate))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.messages[0]", containsString("Username already taken")));
        }

        @Test
        @DisplayName("Should return 400 when username is empty")
        void shouldReturn400_whenUsernameIsEmpty() throws Exception {
            String requestBody = """
                    {
                        "username": "",
                        "password": "password"
                    }
                    """;
            mockMvc.perform(post(SIGN_UP_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when username is empty")
        void shouldReturn400_whenPasswordIsEmpty() throws Exception {
            String requestBody = """
                    {
                        "username": "user",
                        "password": ""
                    }
                    """;
            mockMvc.perform(post(SIGN_UP_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when body is null")
        void shouldReturn400_whenBodyIsNull() throws Exception {
            mockMvc.perform(post(SIGN_UP_PATH)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should access protected endpoint after sign-up")
        void shouldAccessProtectedEndpoint_afterSignUp() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();

            mockMvc.perform(get("/api/resource")
                            .param("path", "docs/file.txt")
                            .cookie(sessionCookie))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class SignInTests {
        private static final String SIGN_IN_PATH = "/api/auth/sign-in";

        @Test
        @DisplayName("Should return 200 when user exists")
        void shouldReturn200_whenUserExists() throws Exception {
            registerAndLoginDefaultUser();

            String signInBody = """
                    {
                        "username": "user",
                        "password": "password"
                    }
                    """;

            mockMvc.perform(post(SIGN_IN_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signInBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("user"));

        }

        @Test
        @DisplayName("Should return 401 when wrong password")
        void shouldReturn401_whenWrongPassword() throws Exception {
            registerAndLoginDefaultUser();

            String signInBody = """
                    {
                        "username": "user",
                        "password": "wrong_password"
                    }
                    """;

            mockMvc.perform(post(SIGN_IN_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signInBody))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.messages[0]").value("Invalid credentials"));
        }

        @Test
        @DisplayName("Should return 401 when user not exists")
        void shouldReturn401_whenUserNotExists() throws Exception {
            String signInBody = """
                    {
                        "username": "user",
                        "password": "password"
                    }
                    """;

            mockMvc.perform(post(SIGN_IN_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signInBody))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.messages[0]").value("Invalid credentials"));
        }

        @Test
        @DisplayName("Should return 400 when username is empty")
        void shouldReturn400_whenUsernameIsEmpty() throws Exception {
            String signInBody = """
                    {
                        "username": "",
                        "password": "password"
                    }
                    """;
            mockMvc.perform(post(SIGN_IN_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signInBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.messages[0]", containsString("username")));
        }

        @Test
        @DisplayName("Should return 400 when password is empty")
        void shouldReturn400_whenPasswordIsEmpty() throws Exception {
            String signInBody = """
                    {
                        "username": "user",
                        "password": ""
                    }
                    """;
            mockMvc.perform(post(SIGN_IN_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signInBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.messages[0]", containsString("password")));
        }

        @Test
        @DisplayName("Should access protected endpoint after sign-in")
        void shouldAccessProtectedEndpoint_afterSignIn() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();

            mockMvc.perform(get("/api/resource")
                            .param("path", "docs/file.txt")
                            .cookie(sessionCookie))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class SingOutTests {
        private static final String PATH_SIGN_OUT = "/api/auth/sign-out";

        @Test
        @DisplayName("Should return 204 when authorized user signs out")
        void shouldReturn204_whenAuthorizedUserSignsOut() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();

            mockMvc.perform(post(PATH_SIGN_OUT)
                            .cookie(sessionCookie))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 401 when unauthorized user access to protected endpoint")
        void shouldReturn401_whenUnauthorizedUserAccessToProtectedEndpoint() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();

            mockMvc.perform(post(PATH_SIGN_OUT)
                            .cookie(sessionCookie))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/resource")
                            .param("path", "docs/file.txt"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
