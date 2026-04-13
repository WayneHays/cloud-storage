package com.waynehays.cloudfilestorage.integration.controller;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResourceControllerTest extends AbstractControllerTest {

    private Cookie registerAndLoginUser(String username, String password) throws Exception {
        String requestBody = buildRequestBody(username, password);
        MvcResult result = mockMvc.perform(post(PATH_SIGN_UP)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn();

        return result.getResponse().getCookie("SESSION");
    }

    private ResultActions getResource(Cookie session, String path) throws Exception {
        return mockMvc.perform(get(PATH_RESOURCE)
                .with(csrf())
                .param(PARAM_PATH, path)
                .cookie(session));
    }

    private ResultActions deleteResource(Cookie session, String path) throws Exception {
        return mockMvc.perform(delete(PATH_RESOURCE)
                .with(csrf())
                .param(PARAM_PATH, path)
                .cookie(session));
    }

    private ResultActions downloadResource(Cookie session, String path) throws Exception {
        return mockMvc.perform(get(PATH_DOWNLOAD)
                .with(csrf())
                .param(PARAM_PATH, path)
                .cookie(session));
    }

    private ResultActions moveResource(Cookie session, String from, String to) throws Exception {
        return mockMvc.perform(put(PATH_MOVE)
                .with(csrf())
                .param(PARAM_FROM, from)
                .param(PARAM_TO, to)
                .cookie(session));
    }

    private ResultActions searchResources(Cookie session, String query) throws Exception {
        return mockMvc.perform(get(PATH_SEARCH)
                .with(csrf())
                .param(PARAM_QUERY, query)
                .cookie(session));
    }

    @Nested
    class UploadTests {

        @Test
        @DisplayName("Should upload file to root and return 201")
        void shouldUploadFileToRoot_andReturn201() throws Exception {
            // given
            Cookie sessionCookie = registerAndLoginDefaultUser();
            MockMultipartFile file = multipartFile("file.txt", "content".getBytes());
            // when & then
            performUpload(sessionCookie, "", file)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$[*].name", hasItem("file.txt")))
                    .andExpect(jsonPath("$[*].type", hasItem("FILE")));
        }

        @Test
        @DisplayName("Should upload file to directory and return 201")
        void shouldUploadFileToDirectory_andReturn201() throws Exception {
            // given
            Cookie sessionCookie = registerAndLoginDefaultUser();
            createDirectory(sessionCookie, "docs/");
            MockMultipartFile file = multipartFile("file.txt", "content".getBytes());

            // when & then
            performUpload(sessionCookie, "docs/", file)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$[*].name", hasItem("file.txt")))
                    .andExpect(jsonPath("$[*].path", hasItem("docs/")));
        }

        @Test
        @DisplayName("Should upload multiple files and return 201")
        void shouldUploadMultipleFiles_andReturn201() throws Exception {
            // given
            Cookie sessionCookie = registerAndLoginDefaultUser();
            createDirectory(sessionCookie, "docs/");
            MockMultipartFile file1 = multipartFile("file1.txt", "content1".getBytes());
            MockMultipartFile file2 = multipartFile("file2.txt", "content2".getBytes());

            // when & then
            performUpload(sessionCookie, "docs/", file1, file2)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$[*].name", hasItems("file1.txt", "file2.txt")));
        }

        @Test
        @DisplayName("Should upload file with nested directory and create structure")
        void shouldUploadFileWithNestedDirectory_andReturn201() throws Exception {
            // given
            Cookie sessionCookie = registerAndLoginDefaultUser();
            MockMultipartFile file = multipartFile("work/report.txt", "content".getBytes());

            // when & then
            performUpload(sessionCookie, "docs/", file)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$[*].name", hasItems("report.txt", "docs/", "work/")));
        }

        @Test
        @DisplayName("Should return 409 when file already exists")
        void shouldReturn409_whenFileAlreadyExists() throws Exception {
            // given
            Cookie sessionCookie = registerAndLoginDefaultUser();
            MockMultipartFile file = multipartFile("file.txt", "content".getBytes());
            performUpload(sessionCookie, "docs/", file);
            MockMultipartFile duplicate = multipartFile("file.txt", "content".getBytes());

            // when & then
            performUpload(sessionCookie, "docs/", duplicate)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("Should return 401 when not authorized")
        void shouldReturn401_whenNotAuthorized() throws Exception {
            // given
            MockMultipartFile file = multipartFile("file.txt", "content".getBytes());

            // when & then
            mockMvc.perform(multipart(PATH_RESOURCE)
                            .with(csrf())
                            .file(file)
                            .param(PARAM_PATH, ""))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class GetInfoTests {

        @Test
        @DisplayName("Should return 200 and file info")
        void shouldReturn200_andFileInfo() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            uploadFile(session, "docs/", "file.txt", "content".getBytes());

            // when & then
            getResource(session, "docs/file.txt")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("file.txt"))
                    .andExpect(jsonPath("$.path").value("docs/"))
                    .andExpect(jsonPath("$.type").value("FILE"))
                    .andExpect(jsonPath("$.size").isNumber());
        }

        @Test
        @DisplayName("Should return 200 and directory info")
        void shouldReturn200_andDirectoryInfo() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            createDirectory(session, "docs/");

            // when & then
            getResource(session, "docs/")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("docs/"))
                    .andExpect(jsonPath("$.path").value(""))
                    .andExpect(jsonPath("$.type").value("DIRECTORY"))
                    .andExpect(jsonPath("$.size").doesNotExist());
        }

        @Test
        @DisplayName("Should return 404 when resource not found")
        void shouldReturn404_whenNotFound() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();

            // when & then
            getResource(session, "nonexistent.txt")
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when path is invalid")
        void shouldReturn400_whenPathInvalid() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();

            // when & then
            getResource(session, "../hack")
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 when not authorized")
        void shouldReturn401_whenNotAuthorized() throws Exception {
            // when & then
            mockMvc.perform(get(PATH_RESOURCE)
                            .with(csrf())
                            .param(PARAM_PATH, "file.txt"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class DeleteTests {

        @Test
        @DisplayName("Should delete file and return 204")
        void shouldDeleteFile_andReturn204() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            uploadFile(session, "docs/", "file.txt", "content".getBytes());

            // when & then
            deleteResource(session, "docs/file.txt")
                    .andExpect(status().isNoContent());

            getResource(session, "docs/file.txt")
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should delete directory with content and return 204")
        void shouldDeleteDirectoryWithContent_andReturn204() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            uploadFile(session, "docs/", "file.txt", "content".getBytes());

            // when & then
            deleteResource(session, "docs/")
                    .andExpect(status().isNoContent());

            getResource(session, "docs/")
                    .andExpect(status().isNotFound());

            getResource(session, "docs/file.txt")
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should delete empty directory and return 204")
        void shouldDeleteEmptyDirectory_andReturn204() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            createDirectory(session, "empty/");

            // when & then
            deleteResource(session, "empty/")
                    .andExpect(status().isNoContent());

            getResource(session, "empty/")
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 when resource not found")
        void shouldReturn404_whenNotFound() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();

            // when & then
            deleteResource(session, "nonexistent.txt")
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 401 when not authorized")
        void shouldReturn401_whenNotAuthorized() throws Exception {
            // when & then
            mockMvc.perform(delete(PATH_RESOURCE)
                            .with(csrf())
                            .param(PARAM_PATH, "file.txt"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class DownloadTests {

        @Test
        @DisplayName("Should download file and return 200")
        void shouldDownloadFile_andReturn200() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            byte[] content = "file content".getBytes();
            uploadFile(session, "docs/", "file.txt", content);

            // when & then
            downloadResource(session, "docs/file.txt")
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("file.txt")))
                    .andExpect(content().bytes(content));
        }

        @Test
        @DisplayName("Should download directory as ZIP and return 200")
        void shouldDownloadDirectoryAsZip_andReturn200() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            uploadFile(session, "docs/", "file1.txt", "content1".getBytes());
            uploadFile(session, "docs/", "file2.txt", "content2".getBytes());

            // when & then
            downloadResource(session, "docs/")
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/zip"))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("docs.zip")));
        }

        @Test
        @DisplayName("Should return 404 when resource not found")
        void shouldReturn404_whenNotFound() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();

            // when & then
            downloadResource(session, "nonexistent.txt")
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 401 when not authorized")
        void shouldReturn401_whenNotAuthorized() throws Exception {
            // when & then
            mockMvc.perform(get(PATH_DOWNLOAD)
                            .with(csrf())
                            .param(PARAM_PATH, "file.txt"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class MoveTests {

        @Test
        @DisplayName("Should rename file and return 200")
        void shouldRenameFile_andReturn200() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            uploadFile(session, "docs/", "old.txt", "content".getBytes());

            // when & then
            moveResource(session, "docs/old.txt", "docs/new.txt")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("new.txt"))
                    .andExpect(jsonPath("$.path").value("docs/"))
                    .andExpect(jsonPath("$.type").value("FILE"));

            getResource(session, "docs/old.txt").andExpect(status().isNotFound());
            getResource(session, "docs/new.txt").andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should move file to another directory and return 200")
        void shouldMoveFileToAnotherDirectory_andReturn200() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            uploadFile(session, "docs/", "file.txt", "content".getBytes());
            createDirectory(session, "archive/");

            // when & then
            moveResource(session, "docs/file.txt", "archive/file.txt")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.path").value("archive/"))
                    .andExpect(jsonPath("$.name").value("file.txt"));

            getResource(session, "docs/file.txt").andExpect(status().isNotFound());
            getResource(session, "archive/file.txt").andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should move directory with content and return 200")
        void shouldMoveDirectoryWithContent_andReturn200() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            createDirectory(session, "target/");
            uploadFile(session, "docs/", "file.txt", "content".getBytes());

            // when & then
            moveResource(session, "docs/", "target/docs/")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("docs/"))
                    .andExpect(jsonPath("$.path").value("target/"))
                    .andExpect(jsonPath("$.type").value("DIRECTORY"));

            getResource(session, "docs/file.txt").andExpect(status().isNotFound());
            getResource(session, "target/docs/file.txt").andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should rename directory and return 200")
        void shouldRenameDirectory_andReturn200() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            uploadFile(session, "docs/", "file.txt", "content".getBytes());

            // when & then
            moveResource(session, "docs/", "documents/")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("documents/"))
                    .andExpect(jsonPath("$.type").value("DIRECTORY"));

            getResource(session, "documents/file.txt").andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 404 when source not found")
        void shouldReturn404_whenSourceNotFound() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();

            // when & then
            moveResource(session, "nonexistent.txt", "other.txt")
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 409 when target already exists")
        void shouldReturn409_whenTargetExists() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            uploadFile(session, "docs/", "file1.txt", "content1".getBytes());
            uploadFile(session, "docs/", "file2.txt", "content2".getBytes());

            // when & then
            moveResource(session, "docs/file1.txt", "docs/file2.txt")
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should return 400 when moving directory to file")
        void shouldReturn400_whenMovingDirectoryToFile() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            createDirectory(session, "docs/");

            // when & then
            moveResource(session, "docs/", "file.txt")
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 when not authorized")
        void shouldReturn401_whenNotAuthorized() throws Exception {
            // when & then
            mockMvc.perform(put(PATH_MOVE)
                            .with(csrf())
                            .param(PARAM_FROM, "file.txt")
                            .param(PARAM_TO, "other.txt"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class SearchTests {

        @Test
        @DisplayName("Should find files by name and return 200")
        void shouldFindFiles_andReturn200() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            uploadFile(session, "docs/", "report.txt", "content".getBytes());
            uploadFile(session, "docs/", "photo.png", "content".getBytes());

            // when & then
            searchResources(session, "report")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name").value("report.txt"));
        }

        @Test
        @DisplayName("Should search case insensitively")
        void shouldSearchCaseInsensitively() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            uploadFile(session, "docs/", "Report.txt", "content".getBytes());

            // when & then
            searchResources(session, "report")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name").value("Report.txt"));
        }

        @Test
        @DisplayName("Should find files in nested directories")
        void shouldFindFilesInNestedDirectories() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            uploadFile(session, "docs/work/", "report.txt", "content".getBytes());

            // when & then
            searchResources(session, "report")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name").value("report.txt"));
        }

        @Test
        @DisplayName("Should return empty list when nothing found")
        void shouldReturnEmptyList_whenNothingFound() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            uploadFile(session, "docs/", "file.txt", "content".getBytes());


            // when & then
            searchResources(session, "nonexistent")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Should return 400 when query is blank")
        void shouldReturn400_whenQueryBlank() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();

            // when & then
            searchResources(session, "")
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 when not authorized")
        void shouldReturn401_whenNotAuthorized() throws Exception {
            // when & then
            mockMvc.perform(get(PATH_SEARCH)
                            .with(csrf())
                            .param(PARAM_QUERY, "test"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class UserIsolationTests {

        @Test
        @DisplayName("Should not find files of another user in search")
        void shouldNotFindFilesOfAnotherUser() throws Exception {
            // given
            Cookie session1 = registerAndLoginUser("user1", "password1");
            uploadFile(session1, "docs/", "secret.txt", "secret".getBytes());

            Cookie session2 = registerAndLoginUser("user2", "password2");

            // when & then
            searchResources(session2, "secret")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Should not access file of another user")
        void shouldNotAccessFileOfAnotherUser() throws Exception {
            // given
            Cookie session1 = registerAndLoginUser("user1", "password1");
            uploadFile(session1, "docs/", "secret.txt", "secret".getBytes());

            Cookie session2 = registerAndLoginUser("user2", "password2");

            // when & then
            getResource(session2, "docs/secret.txt")
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Both users can create directory with same name")
        void shouldAllowSameDirectoryNameForDifferentUsers() throws Exception {
            // given
            Cookie session1 = registerAndLoginUser("user1", "password1");
            Cookie session2 = registerAndLoginUser("user2", "password2");

            // when & then
            createDirectory(session1, "docs/");
            createDirectory(session2, "docs/");

            getDirectoryContent(session1, "docs/").andExpect(status().isOk());
            getDirectoryContent(session2, "docs/").andExpect(status().isOk());
        }
    }
}
