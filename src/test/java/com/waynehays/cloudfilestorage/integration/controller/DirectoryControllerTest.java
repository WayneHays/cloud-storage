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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DirectoryControllerTest extends AbstractRestControllerBaseTest {

    @Nested
    class CreateDirectoryTests {

        @Test
        @DisplayName("Should create directory in root and return 201")
        void shouldCreateDirectoryInRoot_andReturn201() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();

            // when & then
            createDirectoryAndExpectSuccess(session, "docs/");
        }

        @Test
        @DisplayName("Should create nested directory and return 201 when parent exists")
        void shouldCreateNestedDirectory_andReturn201() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            createDirectory(session, "docs/");

            // when & then
            createDirectoryAndExpectSuccess(session, "docs/work/");
        }

        @Test
        @DisplayName("Should return 409 when directory already exists")
        void shouldReturn409_whenDirectoryAlreadyExists() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            createDirectory(session, "docs/");

            // when & then
            mockMvc.perform(post(DIRECTORY_PATH)
                            .param("path", "docs/")
                            .cookie(session))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Resource already exists"));
        }

        @Test
        @DisplayName("Should return 404 when parent directory not exists")
        void shouldReturn404_whenParentNotExists() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();

            // when & then
            mockMvc.perform(post(DIRECTORY_PATH)
                            .param("path", "docs/work/")
                            .cookie(session))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Resource not found"));
        }

        @Test
        @DisplayName("Should return 400 when path is invalid")
        void shouldReturn400_whenPathIsInvalid() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();

            // when & then
            mockMvc.perform(post(DIRECTORY_PATH)
                            .param("path", "../hack/")
                            .cookie(session))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when path does not end with slash")
        void shouldReturn400_whenPathNotDirectory() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();

            // when & then
            mockMvc.perform(post(DIRECTORY_PATH)
                            .param("path", "docs")
                            .cookie(session))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 when user not authorized")
        void shouldReturn401_whenNotAuthorized() throws Exception {
            // when & then
            mockMvc.perform(post(DIRECTORY_PATH)
                            .param("path", "docs/"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class GetContentTests {

        @Test
        @DisplayName("Should return 200 and root content")
        void shouldReturn200_andRootContent() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            createDirectory(session, "docs/");
            createDirectory(session, "images/");

            // when & then
            mockMvc.perform(get(DIRECTORY_PATH)
                            .param("path", "")
                            .cookie(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].name", containsInAnyOrder("docs/", "images/")))
                    .andExpect(jsonPath("$[*].type", everyItem(is("DIRECTORY"))));
        }

        @Test
        @DisplayName("Should return 200 and list of files")
        void shouldReturn200_andListOfFiles() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            uploadFile(session, "docs/", "file1.txt", "content1".getBytes());
            uploadFile(session, "docs/", "file2.txt", "content2".getBytes());
            uploadFile(session, "docs/", "file3.txt", "content3".getBytes());

            // when & then
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
        @DisplayName("Should return 200 and mixed content (files and directories)")
        void shouldReturn200_andMixedContent() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            uploadFile(session, "docs/", "file1.txt", "content1".getBytes());
            uploadFile(session, "docs/work/", "file2.txt", "content2".getBytes());

            // when & then
            mockMvc.perform(get(DIRECTORY_PATH)
                            .param("path", "docs/")
                            .cookie(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].name", containsInAnyOrder("file1.txt", "work/")))
                    .andExpect(jsonPath("$[*].path", everyItem(is("docs/"))));
        }

        @Test
        @DisplayName("Should return only direct children, not recursive")
        void shouldReturnOnlyDirectChildren() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            uploadFile(session, "docs/", "file1.txt", "content1".getBytes());
            uploadFile(session, "docs/work/", "file2.txt", "content2".getBytes());
            uploadFile(session, "docs/work/reports/", "file3.txt", "content3".getBytes());

            // when & then
            mockMvc.perform(get(DIRECTORY_PATH)
                            .param("path", "docs/")
                            .cookie(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].name", containsInAnyOrder("file1.txt", "work/")));
        }

        @Test
        @DisplayName("Should return 200 and empty list when directory is empty")
        void shouldReturn200_andEmptyList() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            createDirectory(session, "empty/");

            // when & then
            mockMvc.perform(get(DIRECTORY_PATH)
                            .param("path", "empty/")
                            .cookie(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Should return 404 when directory not found")
        void shouldReturn404_whenDirectoryNotFound() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();

            // when & then
            mockMvc.perform(get(DIRECTORY_PATH)
                            .param("path", "nonexistent/")
                            .cookie(session))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Resource not found"));
        }

        @Test
        @DisplayName("Should return 401 when user not authorized")
        void shouldReturn401_whenNotAuthorized() throws Exception {
            // when & then
            mockMvc.perform(get(DIRECTORY_PATH)
                            .param("path", "docs/"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
