package com.waynehays.cloudfilestorage.integration.controller.resource.upload;

import com.waynehays.cloudfilestorage.entity.FileInfo;
import com.waynehays.cloudfilestorage.integration.base.AbstractControllerIntegrationTest;
import com.waynehays.cloudfilestorage.integration.controller.resource.TestHelper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FileUploadTest extends AbstractControllerIntegrationTest {
    private static final String UPLOAD_URL = "/api/resource";
    private static final String NAME = "file";
    private static final String FILENAME = "file.txt";
    private static final String CONTENT = "hello world";

    private static final String STORAGE_KEY_FORMAT = "user-%d-files/%s";
    private static final String STORAGE_KEY_FORMAT_WITH_DIR = "user-%d-files/%s/%s";

    private static final String FOLDER_1 = "docs";
    private static final String FOLDER_2 = "work";
    private static final String FOLDER_3 = "task";

    private static final String DIRECTORY = FOLDER_1;
    private static final String NESTED_DIRECTORY = TestHelper.join(FOLDER_1, FOLDER_2, FOLDER_3);

    @Test
    @DisplayName("Should upload file with directory and return 201")
    void shouldUploadFileWithDirectory() throws Exception {
        uploadFile(FILENAME, CONTENT, DIRECTORY)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(FILENAME));

        assertSingleFileUploadedTo(DIRECTORY);
    }

    @Test
    @DisplayName("Should upload file without directory and return 201")
    void shouldUploadFileWithoutDirectory() throws Exception {
        uploadFile(FILENAME, CONTENT, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(FILENAME));

        assertSingleFileUploadedTo(null);
    }

    @Test
    @DisplayName("Should upload file to nested directory and return 201")
    void shouldUploadFileToNestedDirectory() throws Exception {
        uploadFile(FILENAME, CONTENT, NESTED_DIRECTORY)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(FILENAME));

        assertSingleFileUploadedTo(NESTED_DIRECTORY);
    }

    @Test
    @DisplayName("Should upload file with embedded path in filename")
    void shouldUploadFileWithEmbeddedPath() throws Exception {
        uploadFile(TestHelper.join("subfolder", FILENAME), CONTENT, DIRECTORY)
                .andExpect(status().isCreated());

        assertFileCount(1);
    }

    @Test
    @DisplayName("Should upload files with same name to different directories")
    void shouldUploadSameNameToDifferentDirectories() throws Exception {
        uploadFile(FILENAME, CONTENT, "dir1")
                .andExpect(status().isCreated());

        uploadFile(FILENAME, CONTENT, "dir2")
                .andExpect(status().isCreated());

        assertFileCount(2);
    }

    @Test
    @DisplayName("Should return 400 when file is empty")
    void shouldReturn400WhenFileEmpty() throws Exception {
        uploadFile(FILENAME, "", null)
                .andExpect(status().isBadRequest());

        assertFileCount(0);
    }

    @Test
    @DisplayName("Should return 400 when filename contains special characters")
    void shouldReturn400WhenFilenameInvalid() throws Exception {
        uploadFile("file@name.txt", CONTENT, DIRECTORY)
                .andExpect(status().isBadRequest());

        assertFileCount(0);
    }

    @Test
    @DisplayName("Should return 400 when filename contains path traversal")
    void shouldReturn400WhenFilenameHasPathTraversal() throws Exception {
        uploadFile(TestHelper.join("..", FILENAME), CONTENT, DIRECTORY)
                .andExpect(status().isBadRequest());

        assertFileCount(0);
    }

    @Test
    @DisplayName("Should return 400 when directory contains path traversal")
    void shouldReturn400WhenDirectoryHasPathTraversal() throws Exception {
        uploadFile(FILENAME, CONTENT, TestHelper.join("..", FOLDER_1))
                .andExpect(status().isBadRequest());

        assertFileCount(0);
    }

    @Test
    @DisplayName("Should return 400 when directory contains invalid characters")
    void shouldReturn400WhenDirectoryInvalid() throws Exception {
        uploadFile(FILENAME, CONTENT, "my@folder")
                .andExpect(status().isBadRequest());

        assertFileCount(0);
    }

    @Test
    @DisplayName("Should return 409 when duplicate file uploaded")
    void shouldReturn409WhenDuplicate() throws Exception {
        uploadFile(FILENAME, CONTENT, DIRECTORY)
                .andExpect(status().isCreated());

        uploadFile(FILENAME, CONTENT, DIRECTORY)
                .andExpect(status().isConflict());

        assertFileCount(1);
    }

    @Test
    @DisplayName("Should return 401 when not authenticated")
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(createMockFile())
                        .param("path", DIRECTORY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should isolate files between users")
    void shouldIsolateFilesBetweenUsers() throws Exception {
        uploadFile(FILENAME, CONTENT, DIRECTORY)
                .andExpect(status().isCreated());

        Cookie secondUserSessionCookie = registerAndLogin("seconduser", "password456");

        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(createMockFile())
                        .param("path", DIRECTORY)
                        .cookie(secondUserSessionCookie))
                .andExpect(status().isCreated());

        Long secondUserId = getUserId("seconduser");
        List<FileInfo> allFiles = fileInfoRepository.findAll();
        assertThat(allFiles).hasSize(2);

        FileInfo firstUserFile = findFileByUserId(allFiles, userId);
        FileInfo secondUserFile = findFileByUserId(allFiles, secondUserId);

        assertStorageKeyCorrect(firstUserFile.getStorageKey(), userId, DIRECTORY);
        assertStorageKeyCorrect(secondUserFile.getStorageKey(), secondUserId, DIRECTORY);
    }

    private MockMultipartFile createMockFile() {
        return new MockMultipartFile(NAME, FILENAME, MediaType.TEXT_PLAIN_VALUE, CONTENT.getBytes());
    }

    private void assertSingleFileUploadedTo(String directory) throws Exception {
        List<FileInfo> files = fileInfoRepository.findAll();
        assertThat(files).hasSize(1);

        String storageKey = files.getFirst().getStorageKey();
        assertFileExistsInStorage(storageKey);
        assertStorageKeyCorrect(storageKey, userId, directory);
    }

    private void assertFileCount(int expected) {
        assertThat(fileInfoRepository.findAll()).hasSize(expected);
    }

    private void assertFileExistsInStorage(String storageKey) throws Exception {
        Optional<InputStream> stored = fileStorage.get(storageKey);
        assertThat(stored).isPresent();

        try (InputStream is = stored.get()) {
            assertThat(new String(is.readAllBytes())).isEqualTo(CONTENT);
        }
    }

    private void assertStorageKeyCorrect(String actualKey, Long userId, String directory) {
        String expectedKey;

        if (StringUtils.isBlank(directory)) {
            expectedKey = STORAGE_KEY_FORMAT.formatted(userId, FILENAME);
        } else {
            expectedKey = STORAGE_KEY_FORMAT_WITH_DIR.formatted(userId, directory, FILENAME);
        }
        assertThat(actualKey).isEqualTo(expectedKey);
    }

    private FileInfo findFileByUserId(List<FileInfo> files, Long userId) {
        return files.stream()
                .filter(f -> f.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow();
    }
}
