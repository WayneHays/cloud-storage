package com.waynehays.cloudfilestorage.integration.controller.resource.search;

import com.waynehays.cloudfilestorage.integration.base.AbstractControllerIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FileSearchTest extends AbstractControllerIntegrationTest {
    private static final String SEARCH_URL = "/api/resource/search";
    private static final String PARAM_QUERY = "query";

    private static final String FILE_1 = "file1.txt";
    private static final String FILE_2 = "file11.txt";
    private static final String FILE_3 = "image.png";
    private static final String CONTENT = "test";
    private static final String DIRECTORY = "docs";

    @BeforeEach
    void setUpFiles() throws Exception {
        uploadFile(FILE_1, CONTENT, DIRECTORY);
        uploadFile(FILE_2, CONTENT, DIRECTORY);
        uploadFile(FILE_3, CONTENT, DIRECTORY);
    }

    @Test
    @DisplayName("Should find file by name")
    void shouldFindFileByName() throws Exception {
        // when & then
        searchFiles(FILE_1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value(FILE_1));
    }

    @Test
    @DisplayName("Should find files by substring")
    void shouldFindBySubstring() throws Exception {
        // when & then
        searchFiles("file1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("Should return empty list when no match")
    void shouldReturnEmptyList() throws Exception {
        // when & then
        searchFiles("notfound")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should return 401 when not authenticated")
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        // when & then
        mockMvc.perform(get(SEARCH_URL)
                        .param(PARAM_QUERY, FILE_1))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should not return other user's files")
    void shouldNotReturnOtherUsersFiles() throws Exception {
        // given
        Cookie secondUserCookie = registerAndLogin("seconduser", "password456");

        // when & then
        mockMvc.perform(get(SEARCH_URL)
                        .param(PARAM_QUERY, "report")
                        .cookie(secondUserCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private ResultActions searchFiles(String query) throws Exception {
        return mockMvc.perform(get(SEARCH_URL)
                .param(PARAM_QUERY, query)
                .cookie(sessionCookie));
    }
}
