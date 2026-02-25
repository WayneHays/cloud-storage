package com.waynehays.cloudfilestorage.integration.controller.resource;

import com.waynehays.cloudfilestorage.dto.auth.request.SignInRequest;
import com.waynehays.cloudfilestorage.dto.auth.request.SignUpRequest;
import com.waynehays.cloudfilestorage.entity.FileInfo;
import com.waynehays.cloudfilestorage.filestorage.FileStorage;
import com.waynehays.cloudfilestorage.integration.base.AbstractControllerItTest;
import com.waynehays.cloudfilestorage.repository.FileInfoRepository;
import com.waynehays.cloudfilestorage.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ItFileUploadTest extends AbstractControllerItTest {
    private static final String STORAGE_KEY_FORMAT = "user-%d-files/%s";
    private static final String STORAGE_KEY_FORMAT_WITH_DIR = "user-%d-files/%s/%s";
    private static final String UPLOAD_URL = "/api/resource";
    private static final String SIGN_UP_URL = "/api/auth/sign-up";
    private static final String SIGN_IN_URL = "/api/auth/sign-in";
    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "password123";
    private static final String NAME = "file";
    private static final String FILENAME = "file.txt";
    private static final String CONTENT = "hello world";
    private static final String DIRECTORY = "docs";
    private static final String NESTED_DIRECTORY = "docs/work/reports";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileInfoRepository fileInfoRepository;

    @Autowired
    private FileStorage fileStorage;

    @Autowired
    private ObjectMapper objectMapper;

    private Long userId;
    private Cookie sessionCookie;

    @BeforeEach
    void setUp() throws Exception {
        SignUpRequest signUpRequest = new SignUpRequest(USERNAME, PASSWORD);
        mockMvc.perform(post(SIGN_UP_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequest)));

        SignInRequest signInRequest = new SignInRequest(USERNAME, PASSWORD);
        MvcResult result = mockMvc.perform(post(SIGN_IN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signInRequest)))
                .andExpect(status().isOk())
                .andReturn();
        userId = userRepository.findByUsername(USERNAME).orElseThrow().getId();
        sessionCookie = result.getResponse().getCookie("SESSION");
    }

    @AfterEach
    void tearDown() {
        fileInfoRepository.findAll().forEach(fileInfo -> {
            try {
                fileStorage.delete(fileInfo.getStorageKey());
            } catch (Exception ignored) {
            }
        });
        fileInfoRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should upload file with directory and return 201")
    void shouldUploadFileWithDirectory() throws Exception {
        uploadFile(FILENAME, CONTENT, DIRECTORY)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(FILENAME));

        assertSingleFileUploaded(DIRECTORY, FILENAME, CONTENT);
    }

    @Test
    @DisplayName("Should upload file without directory and return 201")
    void shouldUploadFileWithoutDirectory() throws Exception {
        uploadFile(FILENAME, CONTENT, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(FILENAME));

        assertSingleFileUploaded(null, FILENAME, CONTENT);
    }

    @Test
    @DisplayName("Should upload file to nested directory and return 201")
    void shouldUploadFileToNestedDirectory() throws Exception {
        uploadFile(FILENAME, CONTENT, NESTED_DIRECTORY)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(FILENAME));

        assertSingleFileUploaded(NESTED_DIRECTORY, FILENAME, CONTENT);
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
        MockMultipartFile file = createMockFile(FILENAME, CONTENT);

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

        MockMultipartFile file = createMockFile(FILENAME, CONTENT);
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

        assertStorageKeyCorrect(firstUserFile.getStorageKey(), userId, DIRECTORY, FILENAME);
        assertStorageKeyCorrect(secondUserFile.getStorageKey(), secondUserId, DIRECTORY, FILENAME);
    }

    private ResultActions uploadFile(String filename, String content, String directory) throws Exception {
        MockMultipartFile file = createMockFile(filename, content);
        var request = multipart(UPLOAD_URL)
                .file(file)
                .cookie(sessionCookie);

        if (directory != null) {
            request.param("path", directory);
        }

        return mockMvc.perform(request);
    }

    private MockMultipartFile createMockFile(String filename, String content) {
        return new MockMultipartFile(NAME, filename, MediaType.TEXT_PLAIN_VALUE, content.getBytes());
    }

    private Cookie registerAndLogin(String username, String password) throws Exception {
        SignUpRequest signUpRequest = new SignUpRequest(username, password);
        mockMvc.perform(post(SIGN_UP_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequest)));

        SignInRequest signInRequest = new SignInRequest(username, password);
        MvcResult result = mockMvc.perform(post(SIGN_IN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signInRequest)))
                .andReturn();

        return result.getResponse().getCookie("SESSION");
    }

    private void assertSingleFileUploaded(String directory, String filename, String content) throws Exception {
        List<FileInfo> files = findAllFiles();
        assertFileCountInDatabase(files, 1);

        String storageKey = files.getFirst().getStorageKey();
        assertFileExistsInStorage(storageKey, content);
        assertStorageKeyCorrect(storageKey, userId, directory, filename);
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

    private void assertFileExistsInStorage(String storageKey, String expectedContent) throws Exception {
        Optional<InputStream> stored = fileStorage.get(storageKey);
        assertThat(stored).isPresent();

        try (InputStream is = stored.get()) {
            assertThat(new String(is.readAllBytes())).isEqualTo(expectedContent);
        }
    }

    private void assertStorageKeyCorrect(String actualKey, Long userId, String directory, String filename) {
        String expectedKey;

        if (StringUtils.isBlank(directory)) {
            expectedKey = STORAGE_KEY_FORMAT.formatted(userId, filename);
        } else {
            expectedKey = STORAGE_KEY_FORMAT_WITH_DIR.formatted(userId, directory, filename);
        }
        assertThat(actualKey).isEqualTo(expectedKey);
    }
}
