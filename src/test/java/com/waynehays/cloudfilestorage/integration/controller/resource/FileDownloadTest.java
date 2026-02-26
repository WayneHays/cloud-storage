package com.waynehays.cloudfilestorage.integration.controller.resource;

import com.waynehays.cloudfilestorage.constant.Constants;
import com.waynehays.cloudfilestorage.integration.base.AbstractControllerIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FileDownloadTest extends AbstractControllerIntegrationTest {
    private static final String DOWNLOAD_URL = "/api/resource/download";
    private static final String FILENAME = "file.txt";
    private static final String CONTENT = "test";
    private static final String DIRECTORY = "docs";
    private static final String PARAM_PATH = "path";

    @BeforeEach
    void setUpFile() throws Exception {
        uploadFile(FILENAME, CONTENT, DIRECTORY);
    }

    @Test
    @DisplayName("Should download existing file and return 200 with correct content")
    void shouldDownloadFile() throws Exception {
        // when & then
        mockMvc.perform(get(DOWNLOAD_URL)
                        .param(PARAM_PATH, DIRECTORY + Constants.PATH_SEPARATOR + FILENAME)
                        .cookie(sessionCookie))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().string(CONTENT));
    }

    @Test
    @DisplayName("Should download existing file from nested directory with correct content")
    void shouldDownloadFromNestedDirectory() throws Exception {
        // given
        String nestedDirectory = "docs/work/task";
        uploadFile(FILENAME, CONTENT, nestedDirectory);

        // when
        mockMvc.perform(get(DOWNLOAD_URL)
                        .param(PARAM_PATH, nestedDirectory + Constants.PATH_SEPARATOR + FILENAME)
                        .cookie(sessionCookie))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(CONTENT));
    }

    @Test
    @DisplayName("Should return correct content-type and content-length")
    void shouldReturnCorrectContentType() throws Exception {
        // when & then
        mockMvc.perform(get(DOWNLOAD_URL)
                        .param(PARAM_PATH, DIRECTORY + Constants.PATH_SEPARATOR + FILENAME)
                        .cookie(sessionCookie))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(MockMvcResultMatchers.header().longValue("Content-Length", CONTENT.getBytes().length));
    }

    @Test
    @DisplayName("Should return 404 when file not found")
    void shouldReturn404_whenFileNotFound() throws Exception {
        // when & then
        mockMvc.perform(get(DOWNLOAD_URL)
                        .param(PARAM_PATH, DIRECTORY + Constants.PATH_SEPARATOR + "notfound")
                        .cookie(sessionCookie))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 when invalid path")
    void shouldReturn400_whenInvalidPath() throws Exception {
        // when & then
        mockMvc.perform(get(DOWNLOAD_URL)
                        .param(PARAM_PATH, DIRECTORY + Constants.PATH_SEPARATOR + "..")
                        .cookie(sessionCookie))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 401 when user not authenticated")
    void shouldReturn401_whenUserNotAuthenticated() throws Exception {
        // when & then
        mockMvc.perform(get(DOWNLOAD_URL)
                        .param(PARAM_PATH, DIRECTORY + Constants.PATH_SEPARATOR + FILENAME))
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
                        .param(PARAM_PATH, DIRECTORY + Constants.PATH_SEPARATOR + FILENAME)
                        .cookie(secondUserCookie))
                .andExpect(status().isNotFound());
    }
}
