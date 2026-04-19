package com.waynehays.cloudfilestorage.integration.controller;

import com.waynehays.cloudfilestorage.utils.PathUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DirectoryControllerTest extends AbstractControllerTest {

    @Nested
    class CreateDirectoryTests {

        @Test
        @DisplayName("Should create directory in root and return 201")
        void shouldCreateDirectoryInRoot_andReturn201() throws Exception {
            // given
            String path = "docs/";
            String parentPath = PathUtils.extractParentPath(path);
            String name = PathUtils.extractFilename(path);

            // when & then
            createDirectory(sessionCookie, path)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.path").value(parentPath))
                    .andExpect(jsonPath("$.name").value(name))
                    .andExpect(jsonPath("$.size").doesNotExist())
                    .andExpect(jsonPath("$.type").value("DIRECTORY"));
        }

        @Test
        @DisplayName("Should create nested directory and return 201 when parent exists")
        void shouldCreateNestedDirectory_andReturn201() throws Exception {
            // given
            createDirectory(sessionCookie, "docs/")
                    .andExpect(status().isCreated());

            String nestedPath = "docs/work/";
            String parentPath = PathUtils.extractParentPath(nestedPath);
            String name = PathUtils.extractFilename(nestedPath);

            // when & then
            createDirectory(sessionCookie, nestedPath)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.path").value(parentPath))
                    .andExpect(jsonPath("$.name").value(name))
                    .andExpect(jsonPath("$.size").doesNotExist())
                    .andExpect(jsonPath("$.type").value("DIRECTORY"));
        }

        @Test
        @DisplayName("Should return 409 when directory already exists")
        void shouldReturn409_whenDirectoryAlreadyExists() throws Exception {
            // given
            createDirectory(sessionCookie, "docs/")
                    .andExpect(status().isCreated());

            // when & then
            createDirectory(sessionCookie, "docs/")
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should return 404 when parent directory not exists")
        void shouldReturn404_whenParentNotExists() throws Exception {
            // when & then
            createDirectory(sessionCookie, "docs/work/")
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when path is invalid")
        void shouldReturn400_whenPathIsInvalid() throws Exception {
            // when & then
            createDirectory(sessionCookie, "../hack/")
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when path does not end with slash")
        void shouldReturn400_whenPathNotDirectory() throws Exception {
            // when & then
            createDirectory(sessionCookie, "docs")
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 409 when case-sensitive create directory duplicate")
        void shouldReturn409_whenCaseSensitiveDuplicate() throws Exception {
            // given
            createDirectory(sessionCookie, "Photos/");

            // when & then
            createDirectory(sessionCookie, "photos/")
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should return 401 when user not authorized")
        void shouldReturn401_whenNotAuthorized() throws Exception {
            // when & then
            mockMvc.perform(post(PATH_DIRECTORY)
                            .with(csrf())
                            .param(PARAM_PATH, "docs/"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class GetContentTests {

        @Test
        @DisplayName("Should return 200 and root content")
        void shouldReturn200_andRootContent() throws Exception {
            // given
            createDirectory(sessionCookie, "docs/")
                    .andExpect(status().isCreated());
            createDirectory(sessionCookie, "images/")
                    .andExpect(status().isCreated());

            // when & then
            getDirectoryContent(sessionCookie, "")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].name", containsInAnyOrder("docs/", "images/")))
                    .andExpect(jsonPath("$[*].type", everyItem(is("DIRECTORY"))));
        }

        @Test
        @DisplayName("Should return 200 and list of files")
        void shouldReturn200_andListOfFiles() throws Exception {
            // given
            uploadFileAndExpectIsCreated(sessionCookie, "docs/", "file1.txt", "content1".getBytes());
            uploadFileAndExpectIsCreated(sessionCookie, "docs/", "file2.txt", "content2".getBytes());
            uploadFileAndExpectIsCreated(sessionCookie, "docs/", "file3.txt", "content3".getBytes());

            // when & then
            getDirectoryContent(sessionCookie, "docs/")
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
            uploadFileAndExpectIsCreated(sessionCookie, "docs/", "file1.txt", "content1".getBytes());
            uploadFileAndExpectIsCreated(sessionCookie, "docs/work/", "file2.txt", "content2".getBytes());

            // when & then
            getDirectoryContent(sessionCookie, "docs/")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].name", containsInAnyOrder("file1.txt", "work/")))
                    .andExpect(jsonPath("$[*].path", everyItem(is("docs/"))));
        }

        @Test
        @DisplayName("Should return only direct children, not recursive")
        void shouldReturnOnlyDirectChildren() throws Exception {
            // given
            uploadFileAndExpectIsCreated(sessionCookie, "docs/", "file1.txt", "content1".getBytes());
            uploadFileAndExpectIsCreated(sessionCookie, "docs/work/", "file2.txt", "content2".getBytes());
            uploadFileAndExpectIsCreated(sessionCookie, "docs/work/reports/", "file3.txt", "content3".getBytes());

            // when & then
            getDirectoryContent(sessionCookie, "docs/")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].name", containsInAnyOrder("file1.txt", "work/")));
        }

        @Test
        @DisplayName("Should return 200 and empty list when directory is empty")
        void shouldReturn200_andEmptyList() throws Exception {
            // given
            createDirectory(sessionCookie, "empty/")
                    .andExpect(status().isCreated());

            // when & then
            getDirectoryContent(sessionCookie, "empty/")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Should return 404 when directory not found")
        void shouldReturn404_whenDirectoryNotFound() throws Exception {
            // when & then
            getDirectoryContent(sessionCookie, "nonexistent/")
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 401 when user not authorized")
        void shouldReturn401_whenNotAuthorized() throws Exception {
            // when & then
            mockMvc.perform(get(PATH_DIRECTORY)
                            .with(csrf())
                            .param(PARAM_PATH, "docs/"))
                    .andExpect(status().isUnauthorized());

        }
    }
}
