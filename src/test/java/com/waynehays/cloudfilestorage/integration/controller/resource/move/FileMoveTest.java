package com.waynehays.cloudfilestorage.integration.controller.resource.move;

import com.waynehays.cloudfilestorage.integration.base.AbstractControllerIntegrationTest;
import com.waynehays.cloudfilestorage.integration.controller.resource.TestHelper;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FileMoveTest extends AbstractControllerIntegrationTest {
    private static final String MOVE_URL = "/api/resource/move";
    private static final String PARAM_FROM = "from";
    private static final String PARAM_TO = "to";

    private static final String FOLDER_1 = "docs";
    private static final String FOLDER_2 = "work";

    private static final String FILENAME = "file.txt";
    private static final String RENAMED_FILENAME = "renamed.txt";
    private static final String CONTENT = "test";

    private static final String DIRECTORY = FOLDER_1;
    private static final String TARGET_DIRECTORY = FOLDER_2;

    private static final String PATH_FROM = TestHelper.join(DIRECTORY, FILENAME);
    private static final String PATH_TO_RENAMED = TestHelper.join(DIRECTORY, RENAMED_FILENAME);
    private static final String PATH_TO_MOVED = TestHelper.join(TARGET_DIRECTORY, FILENAME);
    private static final String PATH_TO_MOVED_AND_RENAMED = TestHelper.join(TARGET_DIRECTORY, RENAMED_FILENAME);
    public static final String DOWNLOAD_URL = "/api/resource/download";

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Should rename file and return 200")
    void shouldRenameFile() throws Exception {
        // given
        uploadFile(FILENAME, CONTENT, DIRECTORY);

        // when & then
        moveFile(PATH_FROM, PATH_TO_RENAMED)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(RENAMED_FILENAME))
                .andExpect(jsonPath("$.path").value(DIRECTORY));
    }

    @Test
    @DisplayName("Should move file to another directory and return 200")
    void shouldMoveFileToAnotherDirectory() throws Exception {
        // given
        uploadFile(FILENAME, CONTENT, DIRECTORY);

        // when
        moveFile(PATH_FROM, PATH_TO_MOVED)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(FILENAME))
                .andExpect(jsonPath("$.path").value(TARGET_DIRECTORY));
    }

    @Test
    @DisplayName("Should move to another directory and rename file and return 200")
    void shouldMoveAndRenameFile() throws Exception {
        // given
        uploadFile(FILENAME, CONTENT, DIRECTORY);

        // when
        moveFile(PATH_FROM, PATH_TO_MOVED_AND_RENAMED)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(RENAMED_FILENAME))
                .andExpect(jsonPath("$.path").value(TARGET_DIRECTORY));
    }

    @Test
    void shouldUpdateKeyInMinio() throws Exception {
        uploadFile(FILENAME, CONTENT, DIRECTORY);

        moveFile(PATH_FROM, PATH_TO_MOVED);

        mockMvc.perform(get(DOWNLOAD_URL)
                        .param("path", PATH_TO_MOVED)
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(content().string(CONTENT));

        mockMvc.perform(get(DOWNLOAD_URL)
                        .param("path", PATH_FROM)
                        .cookie(sessionCookie))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 when file not exists")
    void shouldReturn404_whenFileNotExists() throws Exception {
        // when & then
        moveFile(PATH_FROM, PATH_TO_MOVED).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 409 when duplicate file")
    void shouldReturn409_whenDuplicate() throws Exception {
        // given
        uploadFile(FILENAME, CONTENT, DIRECTORY);
        uploadFile(FILENAME, CONTENT, TARGET_DIRECTORY);

        // when & then
        moveFile(PATH_FROM, PATH_TO_MOVED).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should return 400 when path is invalid")
    void shouldReturn400WhenInvalidPath() throws Exception {
        uploadFile(FILENAME, CONTENT, DIRECTORY);

        moveFile(TestHelper.join(DIRECTORY, ".."), PATH_TO_MOVED)
                .andExpect(status().isBadRequest());

        moveFile(PATH_FROM, TestHelper.join("my@folder", FILENAME))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 401 when user not authenticated")
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        // when & then
        mockMvc.perform(get(MOVE_URL)
                        .param(PARAM_FROM, PATH_FROM)
                        .param(PARAM_TO, PATH_TO_MOVED))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 404 when moving another user's file")
    void shouldReturn404WhenMovingOtherUsersFile() throws Exception {
        uploadFile(FILENAME, CONTENT, DIRECTORY);

        Cookie secondUserCookie = registerAndLogin("seconduser", "password456");

        mockMvc.perform(get(MOVE_URL)
                        .param(PARAM_FROM, PATH_FROM)
                        .param(PARAM_TO, PATH_TO_MOVED)
                        .cookie(secondUserCookie))
                .andExpect(status().isNotFound());
    }


    private ResultActions moveFile(String from, String to) throws Exception {
        return mockMvc.perform(get(MOVE_URL)
                .param(PARAM_FROM, from)
                .param(PARAM_TO, to)
                .cookie(sessionCookie));
    }
}
