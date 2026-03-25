package com.waynehays.cloudfilestorage.integration.controller;

import com.waynehays.cloudfilestorage.integration.base.AbstractRestControllerBaseTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResourceControllerTest extends AbstractRestControllerBaseTest {
    private static final String MOVE_PATH = RESOURCE_PATH + "/move";
    private static final String DOWNLOAD_PATH = RESOURCE_PATH + "/download";
    private static final String SEARCH_PATH = RESOURCE_PATH + "/search";

    @Nested
    class UploadTests {

        @Test
        @DisplayName("Should upload file to root and return 201")
        void shouldUploadFileToRoot_andReturn201() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            MockMultipartFile file = new MockMultipartFile(
                    "object", "file.txt", "text/plain", "content".getBytes());

            // when & then
            mockMvc.perform(multipart(RESOURCE_PATH)
                            .file(file)
                            .param("path", "")
                            .cookie(session))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$[*].name", hasItem("file.txt")))
                    .andExpect(jsonPath("$[*].type", hasItem("FILE")));
        }

        @Test
        @DisplayName("Should upload file to directory and return 201")
        void shouldUploadFileToDirectory_andReturn201() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            createDirectory(session, "docs/");
            MockMultipartFile file = new MockMultipartFile(
                    "object", "file.txt", "text/plain", "content".getBytes());

            // when & then
            mockMvc.perform(multipart(RESOURCE_PATH)
                            .file(file)
                            .param("path", "docs/")
                            .cookie(session))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$[*].name", hasItem("file.txt")))
                    .andExpect(jsonPath("$[*].path", hasItem("docs/")));
        }

        @Test
        @DisplayName("Should upload multiple files and return 201")
        void shouldUploadMultipleFiles_andReturn201() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            createDirectory(session, "docs/");
            MockMultipartFile file1 = new MockMultipartFile(
                    "object", "file1.txt", "text/plain", "content1".getBytes());
            MockMultipartFile file2 = new MockMultipartFile(
                    "object", "file2.txt", "text/plain", "content2".getBytes());

            // when & then
            mockMvc.perform(multipart(RESOURCE_PATH)
                            .file(file1)
                            .file(file2)
                            .param("path", "docs/")
                            .cookie(session))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$[*].name", hasItems("file1.txt", "file2.txt")));
        }

        @Test
        @DisplayName("Should upload file with nested directory and create structure")
        void shouldUploadFileWithNestedDirectory_andReturn201() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            MockMultipartFile file = new MockMultipartFile(
                    "object", "work/report.txt", "text/plain", "content".getBytes());

            // when & then
            mockMvc.perform(multipart(RESOURCE_PATH)
                            .file(file)
                            .param("path", "docs/")
                            .cookie(session))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$[*].name", hasItems("report.txt", "docs/", "work/")));
        }

        @Test
        @DisplayName("Should return 409 when file already exists")
        void shouldReturn409_whenFileAlreadyExists() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            uploadFile(session, "docs/", "file.txt", "content".getBytes());
            MockMultipartFile duplicate = new MockMultipartFile(
                    "object", "file.txt", "text/plain", "content".getBytes());

            // when & then
            mockMvc.perform(multipart(RESOURCE_PATH)
                            .file(duplicate)
                            .param("path", "docs/")
                            .cookie(session))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("Should return 401 when not authorized")
        void shouldReturn401_whenNotAuthorized() throws Exception {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "object", "file.txt", "text/plain", "content".getBytes());

            // when & then
            mockMvc.perform(multipart(RESOURCE_PATH)
                            .file(file)
                            .param("path", ""))
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
            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "docs/file.txt")
                            .cookie(session))
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
            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "docs/")
                            .cookie(session))
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
            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "nonexistent.txt")
                            .cookie(session))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Resource not found"));
        }

        @Test
        @DisplayName("Should return 400 when path is invalid")
        void shouldReturn400_whenPathInvalid() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();

            // when & then
            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "../hack")
                            .cookie(session))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 when not authorized")
        void shouldReturn401_whenNotAuthorized() throws Exception {
            // when & then
            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "file.txt"))
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
            mockMvc.perform(delete(RESOURCE_PATH)
                            .param("path", "docs/file.txt")
                            .cookie(session))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "docs/file.txt")
                            .cookie(session))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should delete directory with content and return 204")
        void shouldDeleteDirectoryWithContent_andReturn204() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            uploadFile(session, "docs/", "file.txt", "content".getBytes());

            // when & then
            mockMvc.perform(delete(RESOURCE_PATH)
                            .param("path", "docs/")
                            .cookie(session))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "docs/")
                            .cookie(session))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "docs/file.txt")
                            .cookie(session))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should delete empty directory and return 204")
        void shouldDeleteEmptyDirectory_andReturn204() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            createDirectory(session, "empty/");

            // when & then
            mockMvc.perform(delete(RESOURCE_PATH)
                            .param("path", "empty/")
                            .cookie(session))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "empty/")
                            .cookie(session))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 when resource not found")
        void shouldReturn404_whenNotFound() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();

            // when & then
            mockMvc.perform(delete(RESOURCE_PATH)
                            .param("path", "nonexistent.txt")
                            .cookie(session))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 401 when not authorized")
        void shouldReturn401_whenNotAuthorized() throws Exception {
            // when & then
            mockMvc.perform(delete(RESOURCE_PATH)
                            .param("path", "file.txt"))
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
            mockMvc.perform(get(DOWNLOAD_PATH)
                            .param("path", "docs/file.txt")
                            .cookie(session))
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
            mockMvc.perform(get(DOWNLOAD_PATH)
                            .param("path", "docs/")
                            .cookie(session))
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
            mockMvc.perform(get(DOWNLOAD_PATH)
                            .param("path", "nonexistent.txt")
                            .cookie(session))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 401 when not authorized")
        void shouldReturn401_whenNotAuthorized() throws Exception {
            // when & then
            mockMvc.perform(get(DOWNLOAD_PATH)
                            .param("path", "file.txt"))
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
            mockMvc.perform(put(MOVE_PATH)
                            .param("from", "docs/old.txt")
                            .param("to", "docs/new.txt")
                            .cookie(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("new.txt"))
                    .andExpect(jsonPath("$.path").value("docs/"))
                    .andExpect(jsonPath("$.type").value("FILE"));

            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "docs/old.txt")
                            .cookie(session))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "docs/new.txt")
                            .cookie(session))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should move file to another directory and return 200")
        void shouldMoveFileToAnotherDirectory_andReturn200() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            uploadFile(session, "docs/", "file.txt", "content".getBytes());
            createDirectory(session, "archive/");

            // when & then
            mockMvc.perform(put(MOVE_PATH)
                            .param("from", "docs/file.txt")
                            .param("to", "archive/file.txt")
                            .cookie(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.path").value("archive/"))
                    .andExpect(jsonPath("$.name").value("file.txt"));

            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "docs/file.txt")
                            .cookie(session))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "archive/file.txt")
                            .cookie(session))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should move directory with content and return 200")
        void shouldMoveDirectoryWithContent_andReturn200() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            createDirectory(session, "target/");
            uploadFile(session, "docs/", "file.txt", "content".getBytes());

            // when & then
            mockMvc.perform(put(MOVE_PATH)
                            .param("from", "docs/")
                            .param("to", "target/docs/")
                            .cookie(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("docs/"))
                    .andExpect(jsonPath("$.path").value("target/"))
                    .andExpect(jsonPath("$.type").value("DIRECTORY"));

            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "docs/file.txt")
                            .cookie(session))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "target/docs/file.txt")
                            .cookie(session))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should rename directory and return 200")
        void shouldRenameDirectory_andReturn200() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            uploadFile(session, "docs/", "file.txt", "content".getBytes());

            // when & then
            mockMvc.perform(put(MOVE_PATH)
                            .param("from", "docs/")
                            .param("to", "documents/")
                            .cookie(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("documents/"))
                    .andExpect(jsonPath("$.type").value("DIRECTORY"));

            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "documents/file.txt")
                            .cookie(session))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 404 when source not found")
        void shouldReturn404_whenSourceNotFound() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();

            // when & then
            mockMvc.perform(put(MOVE_PATH)
                            .param("from", "nonexistent.txt")
                            .param("to", "other.txt")
                            .cookie(session))
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
            mockMvc.perform(put(MOVE_PATH)
                            .param("from", "docs/file1.txt")
                            .param("to", "docs/file2.txt")
                            .cookie(session))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should return 400 when moving directory to file")
        void shouldReturn400_whenMovingDirectoryToFile() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();
            createDirectory(session, "docs/");

            // when & then
            mockMvc.perform(put(MOVE_PATH)
                            .param("from", "docs/")
                            .param("to", "file.txt")
                            .cookie(session))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 when not authorized")
        void shouldReturn401_whenNotAuthorized() throws Exception {
            // when & then
            mockMvc.perform(put(MOVE_PATH)
                            .param("from", "file.txt")
                            .param("to", "other.txt"))
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
            mockMvc.perform(get(SEARCH_PATH)
                            .param("query", "report")
                            .cookie(session))
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
            mockMvc.perform(get(SEARCH_PATH)
                            .param("query", "report")
                            .cookie(session))
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
            mockMvc.perform(get(SEARCH_PATH)
                            .param("query", "report")
                            .cookie(session))
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
            mockMvc.perform(get(SEARCH_PATH)
                            .param("query", "nonexistent")
                            .cookie(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Should return 400 when query is blank")
        void shouldReturn400_whenQueryBlank() throws Exception {
            // given
            Cookie session = registerAndLoginDefaultUser();

            // when & then
            mockMvc.perform(get(SEARCH_PATH)
                            .param("query", "")
                            .cookie(session))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 when not authorized")
        void shouldReturn401_whenNotAuthorized() throws Exception {
            // when & then
            mockMvc.perform(get(SEARCH_PATH)
                            .param("query", "test"))
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
            mockMvc.perform(get(SEARCH_PATH)
                            .param("query", "secret")
                            .cookie(session2))
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
            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "docs/secret.txt")
                            .cookie(session2))
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

            mockMvc.perform(get(DIRECTORY_PATH)
                            .param("path", "docs/")
                            .cookie(session1))
                    .andExpect(status().isOk());

            mockMvc.perform(get(DIRECTORY_PATH)
                            .param("path", "docs/")
                            .cookie(session2))
                    .andExpect(status().isOk());
        }
    }

    private Cookie registerAndLoginUser(String username, String password) throws Exception {
        String requestBody = """
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(username, password);

        MvcResult result = mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn();

        return result.getResponse().getCookie("SESSION");
    }
}
