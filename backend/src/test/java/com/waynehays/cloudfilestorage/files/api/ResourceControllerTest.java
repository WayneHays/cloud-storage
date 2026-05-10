package com.waynehays.cloudfilestorage.files.api;

import com.waynehays.cloudfilestorage.AbstractControllerTest;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
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

    private ResultActions getResource(Cookie sessionCookie, String path) throws Exception {
        return mockMvc.perform(get(PATH_RESOURCE)
                .with(csrf())
                .param(PARAM_PATH, path)
                .cookie(sessionCookie));
    }

    private ResultActions deleteResource(Cookie sessionCookie, String path) throws Exception {
        return mockMvc.perform(delete(PATH_RESOURCE)
                .with(csrf())
                .param(PARAM_PATH, path)
                .cookie(sessionCookie));
    }

    private ResultActions downloadResource(Cookie sessionCookie, String path) throws Exception {
        return mockMvc.perform(get(PATH_DOWNLOAD)
                .with(csrf())
                .param(PARAM_PATH, path)
                .cookie(sessionCookie));
    }

    private ResultActions moveResource(Cookie sessionCookie, String from, String to) throws Exception {
        return mockMvc.perform(put(PATH_MOVE)
                .with(csrf())
                .param(PARAM_FROM, from)
                .param(PARAM_TO, to)
                .cookie(sessionCookie));
    }

    private ResultActions searchResources(Cookie sessionCookie, String query) throws Exception {
        return mockMvc.perform(get(PATH_SEARCH)
                .with(csrf())
                .param(PARAM_QUERY, query)
                .cookie(sessionCookie));
    }

    @Nested
    class UploadTests {

        @Test
        @DisplayName("Should upload file to root and return 201")
        void shouldUploadFileToRoot_andReturn201() throws Exception {
            // given
            String filename = "file.txt";
            String content = "content";
            byte[] contentBytes = content.getBytes();
            int expectedSize = contentBytes.length;

            MockMultipartFile file = multipartFile(filename, contentBytes);

            // when & then
            performUpload(sessionCookie, "", file)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$[*].name", hasItem(filename)))
                    .andExpect(jsonPath("$[*].type", hasItem("FILE")))
                    .andExpect(jsonPath("$[*].size", hasItem(expectedSize)));
        }

        @Test
        @DisplayName("Should upload file to directory and return 201")
        void shouldUploadFileToDirectory_andReturn201() throws Exception {
            // given
            createDirectory(sessionCookie, "docs/")
                    .andExpect(status().isCreated());
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
            createDirectory(sessionCookie, "docs/")
                    .andExpect(status().isCreated());
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
            MockMultipartFile file = multipartFile("file.txt", "content".getBytes());
            performUpload(sessionCookie, "docs/", file);
            MockMultipartFile duplicate = multipartFile("file.txt", "content".getBytes());

            // when & then
            performUpload(sessionCookie, "docs/", duplicate)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("Should return 409 when lower case filename upload to same directory")
        void shouldReturn409_whenCaseSensitiveFilenameExists() throws Exception {
            // given
            MockMultipartFile file = multipartFile("README.md", "content".getBytes());
            MockMultipartFile expectedDuplicate = multipartFile("readme.md", "content".getBytes());
            performUpload(sessionCookie, "", file);

            // when & then
            performUpload(sessionCookie, "", expectedDuplicate)
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should return 409 when case sensitive duplicate path")
        void shouldReturn409_whenCaseSensitiveDuplicatePath() throws Exception {
            // given
            uploadFileAndExpectIsCreated(sessionCookie, "", "README.md", "content".getBytes());

            // when & then
            uploadFile(sessionCookie, "", "readme.md", "content".getBytes())
                    .andExpect(status().isConflict());
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
    class TypeConflictTests {

        @Test
        @DisplayName("Should return 409 when creating directory with same name as existing file")
        void shouldReturn409WhenCreatingDirectoryWithSameNameAsFile() throws Exception {
            // given
            uploadFileAndExpectIsCreated(sessionCookie, "", "report", "content".getBytes());

            // when & then
            createDirectory(sessionCookie, "report/")
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should return 409 when uploading file with same name as existing directory")
        void shouldReturn409WhenUploadingFileWithSameNameAsDirectory() throws Exception {
            // given
            createDirectory(sessionCookie, "docs/")
                    .andExpect(status().isCreated());

            MockMultipartFile file = multipartFile("docs", "content".getBytes());

            // when & then
            performUpload(sessionCookie, "", file)
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should return 409 when moving file to path where directory with same name exists")
        void shouldReturn409WhenMovingToConflictingType() throws Exception {
            // given
            uploadFileAndExpectIsCreated(sessionCookie, "", "report", "content".getBytes());
            createDirectory(sessionCookie, "docs/")
                    .andExpect(status().isCreated());
            uploadFileAndExpectIsCreated(sessionCookie, "docs/", "data", "content".getBytes());

            // when & then
            moveResource(sessionCookie, "docs/data", "report/")
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    class EdgeCaseTests {

        @Test
        @DisplayName("Should return 404 when moving file to non-existent directory")
        void shouldReturn404WhenMovingToNonExistentDirectory() throws Exception {
            // given
            uploadFileAndExpectIsCreated(sessionCookie, "", "file.txt", "content".getBytes());

            // when & then
            moveResource(sessionCookie, "file.txt", "nonexistent/file.txt")
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 when creating nested directory with missing parent")
        void shouldReturn404WhenCreatingNestedDirectoryWithMissingParent() throws Exception {
            // when & then
            createDirectory(sessionCookie, "a/b/")
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should create all ancestor directories when uploading to deep path")
        void shouldCreateAllAncestorDirectoriesOnUpload() throws Exception {
            // given
            createDirectory(sessionCookie, "a/")
                    .andExpect(status().isCreated());

            // when
            uploadFileAndExpectIsCreated(sessionCookie, "a/b/c/", "file.txt", "content".getBytes());

            // then
            getResource(sessionCookie, "a/b/")
                    .andExpect(status().isOk());
            getResource(sessionCookie, "a/b/c/")
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class GetInfoTests {

        @Test
        @DisplayName("Should return 200 and file info")
        void shouldReturn200_andFileInfo() throws Exception {
            // given
            uploadFileAndExpectIsCreated(sessionCookie, "docs/", "file.txt", "content".getBytes());

            // when & then
            getResource(sessionCookie, "docs/file.txt")
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
            createDirectory(sessionCookie, "docs/")
                    .andExpect(status().isCreated());

            // when & then
            getResource(sessionCookie, "docs/")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("docs/"))
                    .andExpect(jsonPath("$.path").value(""))
                    .andExpect(jsonPath("$.type").value("DIRECTORY"))
                    .andExpect(jsonPath("$.size").doesNotExist());
        }

        @Test
        @DisplayName("Should return 404 when resource not found")
        void shouldReturn404_whenNotFound() throws Exception {
            // when & then
            getResource(sessionCookie, "nonexistent.txt")
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when path is invalid")
        void shouldReturn400_whenPathInvalid() throws Exception {
            // when & then
            getResource(sessionCookie, "../hack")
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 200 when case-sensitive request")
        void shouldReturn200_whenCaseSensitiveRequest() throws Exception {
            // given
            uploadFileAndExpectIsCreated(sessionCookie, "docs/", "File.txt", "content".getBytes());

            // when & then
            getResource(sessionCookie, "docs/file.txt")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", containsString("File.txt")));
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
            uploadFileAndExpectIsCreated(sessionCookie, "docs/", "file.txt", "content".getBytes());

            // when & then
            deleteResource(sessionCookie, "docs/file.txt")
                    .andExpect(status().isNoContent());

            getResource(sessionCookie, "docs/file.txt")
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should delete directory with content and return 204")
        void shouldDeleteDirectoryWithContent_andReturn204() throws Exception {
            // given
            uploadFileAndExpectIsCreated(sessionCookie, "docs/", "file.txt", "content".getBytes());

            // when & then
            deleteResource(sessionCookie, "docs/")
                    .andExpect(status().isNoContent());

            getResource(sessionCookie, "docs/")
                    .andExpect(status().isNotFound());

            getResource(sessionCookie, "docs/file.txt")
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should delete empty directory and return 204")
        void shouldDeleteEmptyDirectory_andReturn204() throws Exception {
            // given
            createDirectory(sessionCookie, "empty/")
                    .andExpect(status().isCreated());

            // when & then
            deleteResource(sessionCookie, "empty/")
                    .andExpect(status().isNoContent());

            getResource(sessionCookie, "empty/")
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 when resource not found")
        void shouldReturn404_whenNotFound() throws Exception {
            // when & then
            deleteResource(sessionCookie, "nonexistent.txt")
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
            byte[] content = "file content".getBytes();
            uploadFileAndExpectIsCreated(sessionCookie, "docs/", "file.txt", content);

            // when & then
            MvcResult mvcResult = downloadResource(sessionCookie, "docs/file.txt")
                    .andExpect(status().isOk())
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(content().bytes(content));


        }

        @Test
        @DisplayName("Should download directory as ZIP and return 200")
        void shouldDownloadDirectoryAsZip_andReturn200() throws Exception {
            // given
            uploadFileAndExpectIsCreated(sessionCookie, "docs/", "file1.txt", "content1".getBytes());
            uploadFileAndExpectIsCreated(sessionCookie, "docs/", "file2.txt", "content2".getBytes());

            // when & then
            downloadResource(sessionCookie, "docs/")
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/zip"))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("docs.zip")));
        }

        @Test
        @DisplayName("Should return 404 when resource not found")
        void shouldReturn404_whenNotFound() throws Exception {
            // when & then
            downloadResource(sessionCookie, "nonexistent.txt")
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
            uploadFileAndExpectIsCreated(sessionCookie, "docs/", "old.txt", "content".getBytes());

            // when & then
            moveResource(sessionCookie, "docs/old.txt", "docs/new.txt")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("new.txt"))
                    .andExpect(jsonPath("$.path").value("docs/"))
                    .andExpect(jsonPath("$.type").value("FILE"));

            getResource(sessionCookie, "docs/old.txt").andExpect(status().isNotFound());
            getResource(sessionCookie, "docs/new.txt").andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should move file to another directory and return 200")
        void shouldMoveFileToAnotherDirectory_andReturn200() throws Exception {
            // given
            uploadFileAndExpectIsCreated(sessionCookie, "docs/", "file.txt", "content".getBytes());
            createDirectory(sessionCookie, "archive/")
                    .andExpect(status().isCreated());

            // when & then
            moveResource(sessionCookie, "docs/file.txt", "archive/file.txt")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.path").value("archive/"))
                    .andExpect(jsonPath("$.name").value("file.txt"));

            getResource(sessionCookie, "docs/file.txt").andExpect(status().isNotFound());
            getResource(sessionCookie, "archive/file.txt").andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should move directory with content and return 200")
        void shouldMoveDirectoryWithContent_andReturn200() throws Exception {
            // given
            createDirectory(sessionCookie, "target/")
                    .andExpect(status().isCreated());
            uploadFileAndExpectIsCreated(sessionCookie, "docs/", "file.txt", "content".getBytes());

            // when & then
            moveResource(sessionCookie, "docs/", "target/docs/")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("docs/"))
                    .andExpect(jsonPath("$.path").value("target/"))
                    .andExpect(jsonPath("$.type").value("DIRECTORY"));

            getResource(sessionCookie, "docs/file.txt").andExpect(status().isNotFound());
            getResource(sessionCookie, "target/docs/file.txt").andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should rename directory and return 200")
        void shouldRenameDirectory_andReturn200() throws Exception {
            // given
            uploadFileAndExpectIsCreated(sessionCookie, "docs/", "file.txt", "content".getBytes());

            // when & then
            moveResource(sessionCookie, "docs/", "documents/")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("documents/"))
                    .andExpect(jsonPath("$.type").value("DIRECTORY"));

            getResource(sessionCookie, "documents/file.txt").andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 404 when source not found")
        void shouldReturn404_whenSourceNotFound() throws Exception {
            // when & then
            moveResource(sessionCookie, "nonexistent.txt", "other.txt")
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 409 when target already exists")
        void shouldReturn409_whenTargetExists() throws Exception {
            // given
            uploadFileAndExpectIsCreated(sessionCookie, "docs/", "file1.txt", "content1".getBytes());
            uploadFileAndExpectIsCreated(sessionCookie, "docs/", "file2.txt", "content2".getBytes());

            // when & then
            moveResource(sessionCookie, "docs/file1.txt", "docs/file2.txt")
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should return 400 when moving directory to file")
        void shouldReturn400_whenMovingDirectoryToFile() throws Exception {
            // given
            createDirectory(sessionCookie, "docs/")
                    .andExpect(status().isCreated());

            // when & then
            moveResource(sessionCookie, "docs/", "file.txt")
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
            uploadFileAndExpectIsCreated(sessionCookie, "docs/", "report.txt", "content".getBytes());
            uploadFileAndExpectIsCreated(sessionCookie, "docs/", "photo.png", "content".getBytes());

            // when & then
            searchResources(sessionCookie, "report")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name").value("report.txt"));
        }

        @Test
        @DisplayName("Should search case insensitively")
        void shouldSearchCaseInsensitively() throws Exception {
            // given
            uploadFileAndExpectIsCreated(sessionCookie, "docs/", "Report.txt", "content".getBytes());

            // when & then
            searchResources(sessionCookie, "report")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name").value("Report.txt"));
        }

        @Test
        @DisplayName("Should find files in nested directories")
        void shouldFindFilesInNestedDirectories() throws Exception {
            // given
            uploadFileAndExpectIsCreated(sessionCookie, "docs/work/", "report.txt", "content".getBytes());

            // when & then
            searchResources(sessionCookie, "report")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name").value("report.txt"));
        }

        @Test
        @DisplayName("Should return empty list when nothing found")
        void shouldReturnEmptyList_whenNothingFound() throws Exception {
            // given
            uploadFileAndExpectIsCreated(sessionCookie, "docs/", "file.txt", "content".getBytes());

            // when & then
            searchResources(sessionCookie, "nonexistent")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Should return 400 when query is blank")
        void shouldReturn400_whenQueryBlank() throws Exception {
            // when & then
            searchResources(sessionCookie, "")
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
            uploadFileAndExpectIsCreated(session1, "docs/", "secret.txt", "secret".getBytes());

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
            uploadFileAndExpectIsCreated(session1, "docs/", "secret.txt", "secret".getBytes());

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
            createDirectory(session1, "docs/")
                    .andExpect(status().isCreated());
            createDirectory(session2, "docs/")
                    .andExpect(status().isCreated());

            getDirectoryContent(session1, "docs/").andExpect(status().isOk());
            getDirectoryContent(session2, "docs/").andExpect(status().isOk());
        }
    }

    @Nested
    class QuotaTests {

        @Test
        @DisplayName("Should release quota after file deletion, allowing re-upload")
        void shouldReleaseQuotaAfterDeletion() throws Exception {
            // given
            byte[] content = new byte[1000];
            uploadFile(sessionCookie, "", "file.txt", content);
            assertThat(getUsedSpaceForDefaultUser()).isEqualTo(1000);

            // when
            deleteResource(sessionCookie, "file.txt")
                    .andExpect(status().isNoContent());

            // then
            assertThat(getUsedSpaceForDefaultUser()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should release quota only for deleted file, not for all files")
        void shouldReleaseQuotaOnlyForDeletedFile() throws Exception {
            // given
            byte[] content100 = new byte[100];
            byte[] content200 = new byte[200];
            uploadFile(sessionCookie, "", "small.txt", content100);
            uploadFile(sessionCookie, "", "large.txt", content200);
            assertThat(getUsedSpaceForDefaultUser()).isEqualTo(300);

            // when
            deleteResource(sessionCookie, "small.txt")
                    .andExpect(status().isNoContent());

            // then
            assertThat(getUsedSpaceForDefaultUser()).isEqualTo(200);
        }
    }
}
