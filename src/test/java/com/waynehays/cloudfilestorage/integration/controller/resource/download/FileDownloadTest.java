package com.waynehays.cloudfilestorage.integration.controller.resource.download;

import com.waynehays.cloudfilestorage.integration.base.AbstractControllerIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FileDownloadTest extends AbstractControllerIntegrationTest {
    private static final String DOWNLOAD_URL = "/api/resource/download";
    private static final String PARAM = "path";

    private static final String FOLDER_1 = "docs";
    private static final String FOLDER_2 = "work";
    private static final String FOLDER_3 = "task";

    private static final String FILENAME = "file.txt";
    private static final String CONTENT = "test";
    private static final String DIRECTORY = FOLDER_1;
    private static final String NESTED_DIRECTORY = join(FOLDER_1, FOLDER_2, FOLDER_3);

    private static final String PATH_TO_FILE = join(DIRECTORY, FILENAME);
    private static final String PATH_TO_NESTED_FILE = join(NESTED_DIRECTORY, FILENAME);

    @BeforeEach
    void setUpFile() throws Exception {
        uploadFile(FILENAME, CONTENT, DIRECTORY);
    }

    @Test
    @DisplayName("Should download existing file and return 200 with correct content")
    void shouldDownloadFile() throws Exception {
        // when & then
        downloadFile(PATH_TO_FILE)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().string(CONTENT));
    }

    @Test
    @DisplayName("Should download existing file from nested directory with correct content")
    void shouldDownloadFromNestedDirectory() throws Exception {
        // given
        uploadFile(FILENAME, CONTENT, NESTED_DIRECTORY);

        // when
        downloadFile(PATH_TO_NESTED_FILE)
                .andExpect(status().isOk())
                .andExpect(content().string(CONTENT));
    }

    @Test
    @DisplayName("Should return correct content-type and content-length")
    void shouldReturnCorrectContentType() throws Exception {
        // when & then
        downloadFile(PATH_TO_FILE)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, CONTENT.getBytes().length));
    }

    @Test
    @DisplayName("Should return 404 when file not found")
    void shouldReturn404_whenFileNotFound() throws Exception {
        // when & then
        downloadFile(join(DIRECTORY, "notfound"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 when invalid path")
    void shouldReturn400_whenInvalidPath() throws Exception {
        // when & then
        downloadFile(join(DIRECTORY, ".."))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 401 when user not authenticated")
    void shouldReturn401_whenUserNotAuthenticated() throws Exception {
        // when & then
        mockMvc.perform(get(DOWNLOAD_URL)
                        .param(PARAM, PATH_TO_FILE))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 404 when download another user file")
    void shouldReturn404_whenDownloadAnotherUserFile() throws Exception {
        // given
        Cookie secondUserCookie = registerAndLogin("secondUser", "password");

        // when & then
        mockMvc.perform(get(DOWNLOAD_URL)
                        .param(PARAM, PATH_TO_FILE)
                        .cookie(secondUserCookie))
                .andExpect(status().isNotFound());
    }

    private ResultActions downloadFile(String path) throws Exception {
        return mockMvc.perform(get(DOWNLOAD_URL)
                .param(PARAM, path)
                .cookie(sessionCookie));
    }
}
