package com.waynehays.cloudfilestorage.unit.controller;

import com.waynehays.cloudfilestorage.security.SecurityConfig;
import com.waynehays.cloudfilestorage.controller.DirectoryController;
import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.security.CustomUserDetails;
import com.waynehays.cloudfilestorage.service.directory.DirectoryServiceApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DirectoryController.class)
@Import(SecurityConfig.class)
class DirectoryControllerTest {
    private static final String PATH = "/api/directory";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DirectoryServiceApi directoryService;

    @Nested
    class GetContentTests {

        @Test
        @DisplayName("Should return 200 with path content")
        void shouldReturnContent_whenDirectoryExists() throws Exception {
            List<ResourceDto> content = List.of(
                    new ResourceDto("docs/", "file.txt", 10L, ResourceType.FILE),
                    new ResourceDto("docs/", "work", null, ResourceType.DIRECTORY)
            );

            when(directoryService.getContent(1L, "docs/"))
                    .thenReturn(content);

            mockMvc.perform(get(PATH)
                            .param("path", "docs/")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].name").value("file.txt"))
                    .andExpect(jsonPath("$[0].type").value("FILE"))
                    .andExpect(jsonPath("$[1].name").value("work"))
                    .andExpect(jsonPath("$[1].type").value("DIRECTORY"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "docs", "file.txt", "/docs/", "docs//work/", "docs/../work/", "docs/wo rk/", "docs/@work/"})
        @DisplayName("Should return 400 when invalid path")
        void shouldReturn400_whenInvalidPath(String path) throws Exception {
            mockMvc.perform(get(PATH)
                            .param("path", path)
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 when path not found")
        void shouldReturn404_whenDirectoryNotFound() throws Exception {
            when(directoryService.getContent(1L, "docs/"))
                    .thenThrow(new ResourceNotFoundException("Directory not found: docs/"));

            mockMvc.perform(get(PATH)
                            .param("path", "docs/")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 401 when user not authorized")
        void shouldReturn401_whenUserNotAuthorized() throws Exception {
            mockMvc.perform(get(PATH)
                            .param("path", "docs/"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 400 when path parameter missing")
        void shouldReturn400_whenPathMissing() throws Exception {
            mockMvc.perform(get(PATH)
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class CreateDirectoryTests {

        @Test
        @DisplayName("Should create path and return 201")
        void shouldCreateDirectoryAndReturn201() throws Exception {
            ResourceDto expectedDto = new ResourceDto("", "docs", null, ResourceType.DIRECTORY);

            when(directoryService.createDirectory(1L, "docs/"))
                    .thenReturn(expectedDto);

            mockMvc.perform(post(PATH)
                            .param("path", "docs/")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.path").value(""))
                    .andExpect(jsonPath("$.name").value("docs"))
                    .andExpect(jsonPath("$.type").value("DIRECTORY"))
                    .andExpect(jsonPath("$.size").doesNotExist());
        }

        @Test
        @DisplayName("Should create nested path and return 201")
        void shouldCreateNestedDirectoryAndReturn201() throws Exception {
            ResourceDto expectedDto = new ResourceDto("docs/", "work", null, ResourceType.DIRECTORY);

            when(directoryService.createDirectory(1L, "docs/work/"))
                    .thenReturn(expectedDto);

            mockMvc.perform(post(PATH)
                            .param("path", "docs/work/")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.path").value("docs/"))
                    .andExpect(jsonPath("$.name").value("work"))
                    .andExpect(jsonPath("$.type").value("DIRECTORY"));
        }

        @Test
        @DisplayName("Should return 409 when path already exists")
        void shouldReturn409_whenDirectoryAlreadyExists() throws Exception {
            when(directoryService.createDirectory(1L, "docs/"))
                    .thenThrow(new ResourceAlreadyExistsException("Directory already exists: docs/"));

            mockMvc.perform(post(PATH)
                            .param("path", "docs/")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should return 404 when parent path not found")
        void shouldReturn404_whenParentNotFound() throws Exception {
            when(directoryService.createDirectory(1L, "docs/work/"))
                    .thenThrow(new ResourceNotFoundException("Parent path not found: docs/"));

            mockMvc.perform(post(PATH)
                            .param("path", "docs/work/")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when path is blank")
        void shouldReturn400_whenPathIsBlank() throws Exception {
            mockMvc.perform(post(PATH)
                            .param("path", "")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when path has no trailing slash")
        void shouldReturn400_whenNoTrailingSlash() throws Exception {
            mockMvc.perform(post(PATH)
                            .param("path", "docs")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isBadRequest());
        }

        @ParameterizedTest
        @ValueSource(strings = {" ", "/docs/", "docs//work/", "docs/../work/", "docs/@work/"})
        @DisplayName("Should return 400 for invalid paths")
        void shouldReturn400_forInvalidPaths(String path) throws Exception {
            mockMvc.perform(post(PATH)
                            .param("path", path)
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 when user not authorized")
        void shouldReturn401_whenUserNotAuthorized() throws Exception {
            mockMvc.perform(post(PATH)
                            .param("path", "docs/"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
