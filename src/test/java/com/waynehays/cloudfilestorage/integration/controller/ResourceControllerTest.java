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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResourceControllerTest extends AbstractRestControllerBaseTest {
    private static final String DOWNLOAD_PATH = RESOURCE_PATH + "/download";

    @Nested
    class GetInfoTests {

        @Test
        @DisplayName("Should return 200 and correct result when file info and resource exists")
        void shouldReturn200_andCorrectResult_whenFileInfo() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();
            uploadFile(sessionCookie, "docs/", "file.txt", "content".getBytes());

            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "docs/file.txt")
                            .cookie(sessionCookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.path").value("docs/"))
                    .andExpect(jsonPath("$.name").value("file.txt"))
                    .andExpect(jsonPath("$.size", greaterThan(0)))
                    .andExpect(jsonPath("$.type").value("FILE"));
        }

        @Test
        @DisplayName("Should return 200 and correct result when directory info and resource exists")
        void shouldReturn200_andCorrectResult_whenDirectoryInfo() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();
            createDirectory(sessionCookie, "docs/");

            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "docs/")
                            .cookie(sessionCookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.path").value(""))
                    .andExpect(jsonPath("$.name").value("docs"))
                    .andExpect(jsonPath("$.size").isEmpty())
                    .andExpect(jsonPath("$.type").value("DIRECTORY"));
        }

        @Test
        @DisplayName("Should return 404 when file not found")
        void shouldReturn404_whenFileNotFound() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();

            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "docs/file.txt")
                            .cookie(sessionCookie))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.messages[0]").value(containsString("not found")));
        }

        @Test
        @DisplayName("Should return 404 when directory not found")
        void shouldReturn404_whenDirectoryNotFound() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();

            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "docs/")
                            .cookie(sessionCookie))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.messages[0]").value(containsString("not found")));
        }

        @Test
        @DisplayName("Should return 401 when unauthorised user")
        void shouldReturn401_whenUnauthorisedUser() throws Exception {
            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "docs/"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class DeleteTests {

        @Test
        @DisplayName("Should delete file and return 204")
        void shouldDeleteFile_andReturn204() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();
            uploadFile(sessionCookie, "docs/", "file.txt", "content".getBytes());

            mockMvc.perform(delete(RESOURCE_PATH)
                            .param("path", "docs/file.txt")
                            .cookie(sessionCookie))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 404 when get info after deleting file")
        void shouldReturn404_whenGetInfo_afterDeletingFile() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();
            uploadFile(sessionCookie, "docs/", "file.txt", "content".getBytes());

            mockMvc.perform(delete(RESOURCE_PATH)
                            .param("path", "docs/file.txt")
                            .cookie(sessionCookie))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "docs/file.txt")
                            .cookie(sessionCookie))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should delete directory with all content and return 204")
        void shouldDeleteDirectoryWithContent() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();
            uploadFile(sessionCookie, "docs/", "file1.txt", "content1".getBytes());
            uploadFile(sessionCookie, "docs/", "file2.txt", "content2".getBytes());

            mockMvc.perform(delete(RESOURCE_PATH)
                            .param("path", "docs/")
                            .cookie(sessionCookie))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "docs/file1.txt")
                            .cookie(sessionCookie))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "docs/file2.txt")
                            .cookie(sessionCookie))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "docs/")
                            .cookie(sessionCookie))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should delete empty directory and return 204")
        void shouldDeleteEmptyDirectory_andReturn204() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();
            createDirectory(sessionCookie, "docs/");

            mockMvc.perform(delete(RESOURCE_PATH)
                            .param("path", "docs/")
                            .cookie(sessionCookie))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "docs/")
                            .cookie(sessionCookie))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 when delete not existent resource")
        void shouldReturn204_whenDeleteNotExistentResource() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();

            mockMvc.perform(delete(RESOURCE_PATH)
                            .param("path", "docs/file.txt")
                            .cookie(sessionCookie))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 401 when unauthorised user")
        void shouldReturn401_whenUnauthorisedUser() throws Exception {
            mockMvc.perform(delete(RESOURCE_PATH)
                            .param("path", "docs/file.txt"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class DownloadTests {

        @Test
        @DisplayName("Should download file and return 200")
        void shouldDownloadFile_andReturn200() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();
            uploadFile(sessionCookie, "docs/", "file.txt", "content".getBytes());

            MvcResult result = mockMvc.perform(get(DOWNLOAD_PATH)
                            .param("path", "docs/file.txt")
                            .cookie(sessionCookie))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"file.txt\""))
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE))
                    .andReturn();

            byte[] responseBytes = result.getResponse().getContentAsByteArray();
            assertThat(responseBytes).isEqualTo("content".getBytes());
        }

        @Test
        @DisplayName("Should download directory and return 200")
        void shouldDownloadDirectory_andReturn200() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();
            uploadFile(sessionCookie, "docs/", "file1.txt", "content".getBytes());

            mockMvc.perform(get(DOWNLOAD_PATH)
                            .param("path", "docs/")
                            .cookie(sessionCookie))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"docs.zip\""))
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/zip"));
        }

        @Test
        @DisplayName("Should return 404 when download not existent resource")
        void shouldReturn404_whenDownloadNotExistentFile() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();

            mockMvc.perform(get(DOWNLOAD_PATH)
                            .param("path", "docs/file.txt")
                            .cookie(sessionCookie))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 401 when user not authorized")
        void shouldReturn401_whenUserNotAuthorised() throws Exception {
            mockMvc.perform(get(DOWNLOAD_PATH)
                            .param("path", "docs/file.txt"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class MoveTests {
        private static final String MOVE_PATH = RESOURCE_PATH + "/move";

        @Test
        @DisplayName("Should move file to another directory when file and target directory exist")
        void shouldMoveFileToAnotherDirectory_whenFileAndTargetDirectoryExist() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();
            uploadFile(sessionCookie, "docs/", "file.txt", "content".getBytes());
            createDirectory(sessionCookie, "work/");

            mockMvc.perform(put(MOVE_PATH)
                            .param("from", "docs/file.txt")
                            .param("to", "work/file.txt")
                            .cookie(sessionCookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.path").value("work/"))
                    .andExpect(jsonPath("$.name").value("file.txt"))
                    .andExpect(jsonPath("$.size").value(greaterThan(0)))
                    .andExpect(jsonPath("$.type").value("FILE"));

            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "docs/file.txt")
                            .cookie(sessionCookie))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "work/file.txt")
                            .cookie(sessionCookie))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should rename file and return 200")
        void shouldRenameFile_andReturn200() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();
            uploadFile(sessionCookie, "docs/", "old.txt", "content".getBytes());

            mockMvc.perform(put(MOVE_PATH)
                            .param("from", "docs/old.txt")
                            .param("to", "docs/new.txt")
                            .cookie(sessionCookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.path").value("docs/"))
                    .andExpect(jsonPath("$.name").value("new.txt"))
                    .andExpect(jsonPath("$.size").value(greaterThan(0)))
                    .andExpect(jsonPath("$.type").value("FILE"));

            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "docs/old.txt")
                            .cookie(sessionCookie))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should move empty directory and return 200")
        void shouldMoveDirectory_andReturn200() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();
            createDirectory(sessionCookie, "docs/");
            createDirectory(sessionCookie, "docs/work/");

            mockMvc.perform(put(MOVE_PATH)
                            .param("from", "docs/work/")
                            .param("to", "work/")
                            .cookie(sessionCookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.path").value(""))
                    .andExpect(jsonPath("$.name").value("work"))
                    .andExpect(jsonPath("$.size").isEmpty())
                    .andExpect(jsonPath("$.type").value("DIRECTORY"));

            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "docs/work/")
                            .cookie(sessionCookie))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should move directory with content and return 200")
        void shouldMoveDirectoryWithContent_andReturn200() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();
            createDirectory(sessionCookie, "docs/");
            createDirectory(sessionCookie, "docs/text/");
            uploadFile(sessionCookie, "docs/work/", "file1.txt", "content1".getBytes());

            mockMvc.perform(put(MOVE_PATH)
                            .param("from", "docs/work/")
                            .param("to", "docs/text/work/")
                            .cookie(sessionCookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.path").value("docs/text/"))
                    .andExpect(jsonPath("$.name").value("work"))
                    .andExpect(jsonPath("$.size").isEmpty())
                    .andExpect(jsonPath("$.type").value("DIRECTORY"));

            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "docs/work/file1.txt")
                            .cookie(sessionCookie))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get(RESOURCE_PATH)
                            .param("path", "docs/text/work/file1.txt")
                            .cookie(sessionCookie))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 404 when from not exists")
        void shouldReturn400_whenFromNotExists() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();
            uploadFile(sessionCookie, "docs/work/text/", "file.txt", "content".getBytes());

            mockMvc.perform(put(MOVE_PATH)
                            .param("from", "docs/text/")
                            .param("to", "docs/test/")
                            .cookie(sessionCookie))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.messages[0]", containsStringIgnoringCase("not found")));
        }

        @Test
        @DisplayName("Should return 409 when file to is already exists")
        void shouldReturn409_whenFileToIsAlreadyExists() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();
            uploadFile(sessionCookie, "docs/work/text/", "file.txt", "content".getBytes());
            uploadFile(sessionCookie, "docs/work/", "file.txt", "content".getBytes());

            mockMvc.perform(put(MOVE_PATH)
                            .param("from", "docs/work/file.txt")
                            .param("to", "docs/work/text/file.txt")
                            .cookie(sessionCookie))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.messages[0]", containsStringIgnoringCase("already exists")));
        }

        @Test
        @DisplayName("Should return 409 when directory to is already exists")
        void shouldReturn409_whenDirectoryToIsAlreadyExists() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();
            createDirectory(sessionCookie, "docs/");
            createDirectory(sessionCookie, "docs/work/");
            createDirectory(sessionCookie, "docs/task/");

            mockMvc.perform(put(MOVE_PATH)
                            .param("from", "docs/work/")
                            .param("to", "docs/task/")
                            .cookie(sessionCookie))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.messages[0]", containsStringIgnoringCase("already exists")));
        }

        @Test
        @DisplayName("Should return 401 when user unauthorised")
        void shouldReturn401_whenUserUnauthorised() throws Exception {
            mockMvc.perform(put(MOVE_PATH)
                            .param("from", "docs/work/")
                            .param("to", "docs/task/"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class SearchTests {
        private static final String SEARCH_PATH = RESOURCE_PATH + "/search";

        @Test
        @DisplayName("Should return results by partial match")
        void shouldReturnResults_byPartialMatch() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();
            uploadFile(sessionCookie, "docs/files/", "file1.txt", "content1".getBytes());
            uploadFile(sessionCookie, "docs/work/", "file2.txt", "content2".getBytes());

            mockMvc.perform(get(SEARCH_PATH)
                            .param("query", "fil")
                            .cookie(sessionCookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[*].name", containsInAnyOrder(
                            "file1.txt", "file2.txt", "files")))
                    .andExpect(jsonPath("$[*].type", hasItems("FILE", "FILE", "DIRECTORY")));
        }

        @Test
        @DisplayName("Should return empty list when nothing found")
        void shouldReturnEmptyList_whenNothingFound() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();
            mockMvc.perform(get(SEARCH_PATH)
                            .param("query", "fil")
                            .cookie(sessionCookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Should return 401 when unauthorized")
        void shouldReturn401_whenUserUnauthorized() throws Exception {
            mockMvc.perform(get(SEARCH_PATH)
                            .param("query", "fil"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class UploadTests {

        @Test
        @DisplayName("Should upload single file and return 201")
        void shouldUploadSingleFile() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();

            MockMultipartFile file1 = new MockMultipartFile(
                    "files",
                    "file1.txt",
                    "text/plain",
                    "file1.txt".getBytes()
            );

            mockMvc.perform(multipart(RESOURCE_PATH)
                            .file(file1)
                            .param("path", "docs/")
                            .cookie(sessionCookie))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$[*].name", containsInAnyOrder("docs", "file1.txt")));
        }

        @Test
        @DisplayName("Should upload list of files with nested directory and return 201")
        void shouldUploadListOfFiles() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();

            MockMultipartFile file1 = new MockMultipartFile(
                    "files",
                    "file1.txt",
                    "text/plain",
                    "file1.txt".getBytes()
            );

            MockMultipartFile file2 = new MockMultipartFile(
                    "files",
                    "work/file2.txt",
                    "text/plain",
                    "file2.txt".getBytes()
            );

            mockMvc.perform(multipart(RESOURCE_PATH)
                            .file(file1)
                            .file(file2)
                            .param("path", "docs/")
                            .cookie(sessionCookie))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$[*].name", containsInAnyOrder(
                            "docs", "work", "file1.txt", "file2.txt")));
        }

        @Test
        @DisplayName("Should return 409 when duplicate")
        void shouldReturn409_whenDuplicate() throws Exception {
            Cookie sessionCookie = registerAndLoginDefaultUser();

            MockMultipartFile original = new MockMultipartFile(
                    "files",
                    "file1.txt",
                    "text/plain",
                    "file1.txt".getBytes()
            );

            MockMultipartFile duplicate = new MockMultipartFile(
                    "files",
                    "file1.txt",
                    "text/plain",
                    "file1.txt".getBytes()
            );

            mockMvc.perform(multipart(RESOURCE_PATH)
                            .file(original)
                            .param("path", "docs/")
                            .cookie(sessionCookie))
                    .andExpect(status().isCreated());

            mockMvc.perform(multipart(RESOURCE_PATH)
                            .file(duplicate)
                            .param("path", "docs/")
                            .cookie(sessionCookie))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should return 401 when user unauthorised")
        void shouldReturn401_whenUserUnauthorised() throws Exception {
            MockMultipartFile file1 = new MockMultipartFile(
                    "files",
                    "file1.txt",
                    "text/plain",
                    "file1.txt".getBytes()
            );

            mockMvc.perform(multipart(RESOURCE_PATH)
                            .file(file1)
                            .param("path", "docs/"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
