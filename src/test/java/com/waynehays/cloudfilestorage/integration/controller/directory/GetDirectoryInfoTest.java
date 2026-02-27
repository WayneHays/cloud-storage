package com.waynehays.cloudfilestorage.integration.controller.directory;

import com.waynehays.cloudfilestorage.dto.file.response.ResourceType;
import com.waynehays.cloudfilestorage.integration.base.AbstractControllerIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GetDirectoryInfoTest extends AbstractControllerIntegrationTest {
    private static final String DIRECTORY_INFO_URL = "/api/directory";
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
    @DisplayName("Should get info and return correct data and 200")
    void shouldGetInfo() throws Exception {
        // given
        uploadFile(FILENAME, CONTENT, DIRECTORY);

        // when
        getResourceInfo(DIRECTORY)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").value(FILENAME))
                .andExpect(jsonPath("$[0].path").value(DIRECTORY))
                .andExpect(jsonPath("$[0].type").value(ResourceType.FILE.toString()));
    }

    @Test
    @DisplayName("Should get info from nested directory")
    void shouldGetInfoFromNestedDirectory() throws Exception {
        // given
        uploadFile(FILENAME, CONTENT, NESTED_DIRECTORY);

        // when
        getResourceInfo(NESTED_DIRECTORY)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value(FILENAME))
                .andExpect(jsonPath("$[0].path").value(NESTED_DIRECTORY));
    }

    @Test
    @DisplayName("Should get info from directory: files and directories")
    void shouldGetFilesAndDirectories() throws Exception {
        // given
        uploadFile(FILENAME, CONTENT, DIRECTORY);
        uploadFile(FILENAME, CONTENT, NESTED_DIRECTORY);

        // when
        getResourceInfo(DIRECTORY)
                .andDo(print())
                .andExpect(status().isOk());
    }


    private ResultActions getResourceInfo(String directory) throws Exception {
        return mockMvc.perform(get(DIRECTORY_INFO_URL)
                .param(PARAM, directory)
                .cookie(sessionCookie));
    }
}
