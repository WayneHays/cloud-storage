package com.waynehays.cloudfilestorage.core.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("Should create user with username and password")
        void shouldCreateUser() {
            User user = new User("john", "encoded");

            assertThat(user.getUsername()).isEqualTo("john");
            assertThat(user.getPassword()).isEqualTo("encoded");
        }

        @Test
        @DisplayName("Should throw NPE when username is null")
        void shouldThrowWhenUsernameNull() {
            assertThatThrownBy(() -> new User(null, "pwd"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("username");
        }

        @Test
        @DisplayName("Should throw NPE when password is null")
        void shouldThrowWhenPasswordNull() {
            assertThatThrownBy(() -> new User("user", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("password");
        }
    }

    @Nested
    @DisplayName("Setters")
    class Setters {

        @Test
        @DisplayName("Should update username")
        void shouldUpdateUsername() {
            User user = new User("old", "pwd");

            user.setUsername("new");

            assertThat(user.getUsername()).isEqualTo("new");
        }

        @Test
        @DisplayName("Should update password")
        void shouldUpdatePassword() {
            User user = new User("user", "old");

            user.setPassword("new");

            assertThat(user.getPassword()).isEqualTo("new");
        }

        @Test
        @DisplayName("Should throw NPE when setting username to null")
        void shouldThrowWhenSettingUsernameNull() {
            User user = new User("user", "pwd");

            assertThatThrownBy(() -> user.setUsername(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("username");
        }

        @Test
        @DisplayName("Should throw NPE when setting password to null")
        void shouldThrowWhenSettingPasswordNull() {
            User user = new User("user", "pwd");

            assertThatThrownBy(() -> user.setPassword(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("password");
        }
    }
}