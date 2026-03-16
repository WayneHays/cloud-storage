package com.waynehays.cloudfilestorage.integration.controller;

import com.waynehays.cloudfilestorage.integration.base.AbstractRestControllerBaseTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DirectoryControllerTest extends AbstractRestControllerBaseTest {

    @Nested
    class CreateDirectoryTests {

        @Test
        @DisplayName("Should create path in root and return 201")
        void shouldCreateDirectoryInRoot_andReturn201() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();
            createDirectoryAndExpectSuccess(sessionCookie, "docs/");
        }

        @Test
        @DisplayName("Should create nested path and return 201 when parent path exists")
        void shouldCreateNestedDirectory_andReturn201_whenParentDirectoryExists() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();
            createDirectory(sessionCookie, "docs/");
            createDirectoryAndExpectSuccess(sessionCookie, "docs/work/");
        }

        @Test
        @DisplayName("Should return 409 when path already exists")
        void shouldReturn409_whenDirectoryAlreadyExists() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();
            createDirectory(sessionCookie, "docs/");

            mockMvc.perform(post(DIRECTORY_PATH)
                            .param("path", "docs/")
                            .cookie(sessionCookie))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.messages[0]").value("Resource already exists"));
        }

        @Test
        @DisplayName("Should return 404 when creating nested path when parent not exists")
        void shouldReturn404_whenParentDirectoryNotExists() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();

            mockMvc.perform(post(DIRECTORY_PATH)
                            .param("path", "docs/work/")
                            .cookie(sessionCookie))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.messages[0]").value("Resource not found"));
        }

        @Test
        @DisplayName("Should return 401 when user not authorized")
        void shouldReturn401_whenUserNotAuthorised() throws Exception {
            mockMvc.perform(post(DIRECTORY_PATH)
                            .param("path", "docs/work/"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class GetContentTests {

        @Test
        @DisplayName("Should return 200 and list of resources when directory contains files")
        void shouldReturn200_andListOfResources() throws Exception {
            Cookie session = registerAndLoginDefaultUser();

            uploadFile(session, "docs/", "file1.txt", "content1".getBytes());
            uploadFile(session, "docs/", "file2.txt", "content2".getBytes());
            uploadFile(session, "docs/", "file3.txt", "content3".getBytes());

            mockMvc.perform(get(DIRECTORY_PATH)
                            .param("path", "docs/")
                            .cookie(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[*].name", containsInAnyOrder("file1.txt", "file2.txt", "file3.txt")))
                    .andExpect(jsonPath("$[*].type", everyItem(is("FILE"))))
                    .andExpect(jsonPath("$[*].path", everyItem(is("docs/"))));
        }

        @Test
        @DisplayName("Should return 200 and list of resources when directory contains files and nested directory")
        void shouldReturn200_andListOfResources_whenDirectoryContainsFilesAndNestedDirectory() throws Exception {
            Cookie session = registerAndLoginDefaultUser();

            uploadFile(session, "docs/", "file1.txt", "content1".getBytes());
            uploadFile(session, "docs/work/", "file2.txt", "content2".getBytes());

            mockMvc.perform(get(DIRECTORY_PATH)
                            .param("path", "docs/")
                            .cookie(session))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].name", containsInAnyOrder("file1.txt", "work")))
                    .andExpect(jsonPath("$[*].path", everyItem(is("docs/"))));
        }

        @Test
        @DisplayName("Should return 200 and empty list when directory is empty")
        void shouldReturn200_andEmptyList_whenDirectoryIsEmpty() throws Exception {
            Cookie session = registerAndLoginDefaultUser();
            createDirectory(session, "empty/");

            mockMvc.perform(get(DIRECTORY_PATH)
                            .param("path", "empty/")
                            .cookie(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Should return 404 when directory not found")
        void shouldReturn404_whenDirectoryNotFound() throws Exception {
            Cookie session = registerAndLoginDefaultUser();

            mockMvc.perform(get(DIRECTORY_PATH)
                            .param("path", "nonexistent/")
                            .cookie(session))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 401 when user not authorized")
        void shouldReturn401_whenNotAuthorized() throws Exception {
            mockMvc.perform(get(DIRECTORY_PATH)
                            .param("path", "docs/"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
