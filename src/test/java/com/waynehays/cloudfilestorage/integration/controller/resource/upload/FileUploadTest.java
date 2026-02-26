package com.waynehays.cloudfilestorage.integration.controller.resource.upload;

import com.waynehays.cloudfilestorage.entity.FileInfo;
import com.waynehays.cloudfilestorage.integration.base.AbstractControllerIntegrationTest;
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
    private static final String STORAGE_KEY_FORMAT = "user-%d-files/%s";
    private static final String STORAGE_KEY_FORMAT_WITH_DIR = "user-%d-files/%s/%s";
    private static final String UPLOAD_URL = "/api/resource";
    private static final String NAME = "file";
    private static final String FILENAME = "file.txt";
    private static final String CONTENT = "hello world";
    private static final String DIRECTORY = "docs";
    private static final String NESTED_DIRECTORY = "docs/work/reports";

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
        String embeddedFilename = "subfolder/file.txt";

        uploadFile(embeddedFilename, CONTENT, DIRECTORY)
                .andExpect(status().isCreated());

        List<FileInfo> files = findAllFiles();
        assertFileCountInDatabase(files, 1);
    }

    @Test
    @DisplayName("Should upload files with same name to different directories")
    void shouldUploadSameNameToDifferentDirectories() throws Exception {
        uploadFile(FILENAME, CONTENT, "dir1")
                .andExpect(status().isCreated());

        uploadFile(FILENAME, CONTENT, "dir2")
                .andExpect(status().isCreated());

        List<FileInfo> files = findAllFiles();
        assertFileCountInDatabase(files, 2);
    }

    @Test
    @DisplayName("Should return 400 when file is empty")
    void shouldReturn400WhenFileEmpty() throws Exception {
        uploadFile(FILENAME, "", null)
                .andExpect(status().isBadRequest());

        assertNoFilesUploaded();
    }

    @Test
    @DisplayName("Should return 400 when filename contains special characters")
    void shouldReturn400WhenFilenameInvalid() throws Exception {
        uploadFile("file@name.txt", CONTENT, DIRECTORY)
                .andExpect(status().isBadRequest());

        assertNoFilesUploaded();
    }

    @Test
    @DisplayName("Should return 400 when filename contains path traversal")
    void shouldReturn400WhenFilenameHasPathTraversal() throws Exception {
        uploadFile("../file.txt", CONTENT, DIRECTORY)
                .andExpect(status().isBadRequest());

        assertNoFilesUploaded();
    }

    @Test
    @DisplayName("Should return 400 when directory contains path traversal")
    void shouldReturn400WhenDirectoryHasPathTraversal() throws Exception {
        uploadFile(FILENAME, CONTENT, "../docs")
                .andExpect(status().isBadRequest());

        assertNoFilesUploaded();
    }

    @Test
    @DisplayName("Should return 400 when directory contains invalid characters")
    void shouldReturn400WhenDirectoryInvalid() throws Exception {
        uploadFile(FILENAME, CONTENT, "my@folder")
                .andExpect(status().isBadRequest());

        assertNoFilesUploaded();
    }

    @Test
    @DisplayName("Should return 409 when duplicate file uploaded")
    void shouldReturn409WhenDuplicate() throws Exception {
        uploadFile(FILENAME, CONTENT, DIRECTORY)
                .andExpect(status().isCreated());

        uploadFile(FILENAME, CONTENT, DIRECTORY)
                .andExpect(status().isConflict());

        List<FileInfo> files = findAllFiles();
        assertFileCountInDatabase(files, 1);
    }

    @Test
    @DisplayName("Should return 401 when not authenticated")
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        MockMultipartFile file = createMockFile();

        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(file)
                        .param("path", DIRECTORY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should isolate files between users")
    void shouldIsolateFilesBetweenUsers() throws Exception {
        uploadFile(FILENAME, CONTENT, DIRECTORY)
                .andExpect(status().isCreated());

        Cookie secondUserSessionCookie = registerAndLogin("seconduser", "password456");

        MockMultipartFile file = createMockFile();
        mockMvc.perform(multipart(UPLOAD_URL)
                        .file(file)
                        .param("path", DIRECTORY)
                        .cookie(secondUserSessionCookie))
                .andExpect(status().isCreated());

        Long secondUserId = userRepository.findByUsername("seconduser").orElseThrow().getId();

        List<FileInfo> allFiles = findAllFiles();
        assertFileCountInDatabase(allFiles, 2);

        FileInfo firstUserFile = allFiles.stream()
                .filter(f -> f.getUser().getId().equals(userId))
                .findFirst().orElseThrow();
        FileInfo secondUserFile = allFiles.stream()
                .filter(f -> f.getUser().getId().equals(secondUserId))
                .findFirst().orElseThrow();

        assertStorageKeyCorrect(firstUserFile.getStorageKey(), userId, DIRECTORY);
        assertStorageKeyCorrect(secondUserFile.getStorageKey(), secondUserId, DIRECTORY);
    }

    private MockMultipartFile createMockFile() {
        return new MockMultipartFile(NAME, FILENAME, MediaType.TEXT_PLAIN_VALUE, CONTENT.getBytes());
    }

    private void assertSingleFileUploadedTo(String directory) throws Exception {
        List<FileInfo> files = findAllFiles();
        assertFileCountInDatabase(files, 1);

        String storageKey = files.getFirst().getStorageKey();
        assertFileExistsInStorage(storageKey);
        assertStorageKeyCorrect(storageKey, userId, directory);
    }

    private void assertNoFilesUploaded() {
        List<FileInfo> files = findAllFiles();
        assertFileCountInDatabase(files, 0);
    }

    private List<FileInfo> findAllFiles() {
        return fileInfoRepository.findAll();
    }

    private void assertFileCountInDatabase(List<FileInfo> files, int expected) {
        assertThat(files).hasSize(expected);
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
}
