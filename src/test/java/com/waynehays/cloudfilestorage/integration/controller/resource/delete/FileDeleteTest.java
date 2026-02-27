package com.waynehays.cloudfilestorage.integration.controller.resource.delete;

import com.waynehays.cloudfilestorage.integration.base.AbstractControllerIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FileDeleteTest extends AbstractControllerIntegrationTest {
    private static final String DELETE_URL = "/api/resource";
    private static final String PARAM = "path";

    private static final String FOLDER_1 = "docs";
    private static final String FOLDER_2 = "work";

    private static final String FILENAME = "file.txt";
    private static final String CONTENT = "test";

    private static final String DIRECTORY = FOLDER_1;
    private static final String NESTED_DIRECTORY = join(FOLDER_1, FOLDER_2);

    private static final String PATH_TO_FILE = join(DIRECTORY, FILENAME);
    private static final String PATH_TO_NESTED_FILE = join(NESTED_DIRECTORY, FILENAME);

    @Test
    @DisplayName("Should delete file and return 204")
    void shouldDeleteFile() throws Exception {
        // given
        uploadFile(FILENAME, CONTENT, DIRECTORY);

        // when
        deleteFile(PATH_TO_FILE).andExpect(status().isNoContent());

        // then
        assertThat(fileInfoRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should delete file from nested directory and return 204")
    void shouldDeleteFileFromNestedDirectory() throws Exception {
        // given
        uploadFile(FILENAME, CONTENT, NESTED_DIRECTORY);

        // when
        deleteFile(PATH_TO_NESTED_FILE).andExpect(status().isNoContent());

        // then
        assertThat(fileInfoRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should return 404 when file not found")
    void shouldReturn404_whenFileNotFound() throws Exception {
        // when
        deleteFile(PATH_TO_FILE).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 when path invalid path")
    void shouldReturn400_whenInvalidPath() throws Exception {
        // when
        deleteFile("../docs").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 401 when user not authenticated")
    void shouldReturn401_whenUserNotAuthenticated() throws Exception {
        // given
        Cookie notAuthUserCookie = new Cookie("bad", "");

        // when
        mockMvc.perform(delete(DELETE_URL)
                        .param(PARAM, PATH_TO_FILE)
                        .cookie(notAuthUserCookie))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 404 when deleting another user's file")
    void shouldReturn404WhenDeletingOtherUsersFile() throws Exception {
        // given
        uploadFile(FILENAME, CONTENT, DIRECTORY);

        Cookie secondUserCookie = registerAndLogin("seconduser", "password456");

        // when
        mockMvc.perform(delete(DELETE_URL)
                        .param(PARAM, PATH_TO_FILE)
                        .cookie(secondUserCookie))
                .andExpect(status().isNotFound());

        // then
        assertThat(fileInfoRepository.findAll()).hasSize(1);
    }

    private ResultActions deleteFile(String path) throws Exception {
        return mockMvc.perform(delete(DELETE_URL)
                .param(PARAM, path)
                .cookie(sessionCookie));
    }
}
