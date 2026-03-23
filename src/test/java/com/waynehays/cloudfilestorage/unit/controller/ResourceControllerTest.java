package com.waynehays.cloudfilestorage.unit.controller;

import com.waynehays.cloudfilestorage.component.parser.MultipartFileDataParserApi;
import com.waynehays.cloudfilestorage.security.SecurityConfig;
import com.waynehays.cloudfilestorage.controller.ResourceController;
import com.waynehays.cloudfilestorage.dto.FileData;
import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.dto.response.DownloadResult;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.exception.InvalidMoveException;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.security.CustomUserDetails;
import com.waynehays.cloudfilestorage.service.resource.ResourceServiceApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResourceController.class)
@Import(SecurityConfig.class)
class ResourceControllerTest {
    private static final String PATH = "/api/resource";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MultipartFileDataParserApi multipartFileDataParser;

    @MockitoBean
    private ResourceServiceApi resourceService;

    @Nested
    class GetInfoTests {
        @Test
        @DisplayName("Should return 200 and correct info when path to file is valid")
        void shouldReturn200AndCorrectInfo_whenPathIsValid() throws Exception {
            // given
            ResourceDto expectedResult = new ResourceDto(
                    "docs/file.txt",
                    "file.txt",
                    10L,
                    ResourceType.FILE
            );
            when(resourceService.getInfo(1L, "docs/file.txt"))
                    .thenReturn(expectedResult);

            // when & then
            mockMvc.perform(get(PATH)
                            .param("path", "docs/file.txt")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value(expectedResult.name()))
                    .andExpect(jsonPath("$.path").value(expectedResult.path()))
                    .andExpect(jsonPath("$.size").value(expectedResult.size()))
                    .andExpect(jsonPath("$.type").value(expectedResult.type().toString()));
        }

        @Test
        @DisplayName("Should return 200 and correct info when path to path is valid")
        void shouldReturn200AndCorrectInfo_whenPathToDirectoryIsValid() throws Exception {
            // given
            ResourceDto expectedResult = new ResourceDto(
                    "docs/",
                    "docs",
                    null,
                    ResourceType.DIRECTORY
            );
            when(resourceService.getInfo(1L, "docs/"))
                    .thenReturn(expectedResult);

            // when & then
            mockMvc.perform(get(PATH)
                            .param("path", "docs/")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value(expectedResult.name()))
                    .andExpect(jsonPath("$.path").value(expectedResult.path()))
                    .andExpect(jsonPath("$.size").value(expectedResult.size()))
                    .andExpect(jsonPath("$.type").value(expectedResult.type().toString()));
        }

        @Test
        @DisplayName("Should return 404 when resource not found")
        void shouldReturn404_whenResourceNotFound() throws Exception {
            // given
            when(resourceService.getInfo(1L, "docs/"))
                    .thenThrow(new ResourceNotFoundException("Resource not found: docs/"));

            // when & then
            mockMvc.perform(get(PATH)
                            .param("path", "docs/")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when empty path")
        void shouldReturn400_whenEmptyPath() throws Exception {
            mockMvc.perform(get(PATH)
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "/docs/", "docs//work/", "docs/../work/", "docs/wo rk/", "docs/@work/"})
        @DisplayName("Should return 400 when invalid path")
        void shouldReturn400_whenInvalidPath(String path) throws Exception {
            mockMvc.perform(get(PATH)
                            .param("path", path)
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 when user not authorized")
        void shouldReturn401_whenUserNotAuthorized() throws Exception {
            mockMvc.perform(get(PATH)
                            .param("path", "docs/"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class DeleteTests {

        @Test
        @DisplayName("Should delete resource when valid path")
        void shouldDeleteResource() throws Exception {
            mockMvc.perform(delete(PATH)
                            .param("path", "docs/file.txt")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 404 when resource not found")
        void shouldReturn404_whenResourceNotFound() throws Exception {
            // given
            doThrow(new ResourceNotFoundException("Resource not found"))
                    .when(resourceService).delete(1L, "docs/file.txt");

            // when & then
            mockMvc.perform(delete(PATH)
                            .param("path", "docs/file.txt")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isNotFound());
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "/docs/", "docs//work/", "docs/../work/", "docs/wo rk/", "docs/@work/"})
        @DisplayName("Should return 400 when invalid path")
        void shouldReturn400_whenInvalidPath(String path) throws Exception {
            mockMvc.perform(delete(PATH)
                            .param("path", path)
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 when user not authorized")
        void shouldReturn401_whenUserNotAuthorized() throws Exception {
            mockMvc.perform(delete(PATH)
                            .param("path", "docs/file.txt"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class DownloadTests {
        private static final String DOWNLOAD_PATH = "/api/resource/download";

        @Test
        @DisplayName("Should download file and return 200")
        void shouldDownloadFileAndReturn200() throws Exception {
            // given
            StreamingResponseBody body = outputStream -> outputStream.write("content".getBytes());
            DownloadResult result = new DownloadResult(body, "file.txt", "application/octet-stream");

            when(resourceService.download(1L, "docs/file.txt"))
                    .thenReturn(result);

            // when & then
            mockMvc.perform(get(DOWNLOAD_PATH)
                            .param("path", "docs/file.txt")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/octet-stream"))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"file.txt\""));
        }

        @Test
        @DisplayName("Should download path and return 200")
        void shouldDownloadDirectoryAndReturn200() throws Exception {
            // given
            StreamingResponseBody body = outputStream -> outputStream.write("content".getBytes());
            DownloadResult result = new DownloadResult(body, "docs", "application/zip");

            when(resourceService.download(1L, "docs/"))
                    .thenReturn(result);

            // when & then
            mockMvc.perform(get(DOWNLOAD_PATH)
                            .param("path", "docs/")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/zip"))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"docs\""));
        }

        @Test
        @DisplayName("Should return 404 when resource not found")
        void shouldReturn404_whenResourceNotFound() throws Exception {
            // given
            doThrow(new ResourceNotFoundException("Resource not found"))
                    .when(resourceService).download(1L, "docs/file.txt");

            // when & then
            mockMvc.perform(get(DOWNLOAD_PATH)
                            .param("path", "docs/file.txt")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isNotFound());
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "/docs/", "docs//work/", "docs/../work/", "docs/wo rk/", "docs/@work/"})
        @DisplayName("Should return 400 when invalid path")
        void shouldReturn400_whenInvalidPath(String path) throws Exception {
            mockMvc.perform(get(DOWNLOAD_PATH)
                            .param("path", path)
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 when user not authorized")
        void shouldReturn401_whenUserNotAuthorized() throws Exception {
            mockMvc.perform(get(DOWNLOAD_PATH)
                            .param("path", "docs/file.txt"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class MoveTests {
        private static final String MOVE_PATH = "/api/resource/move";

        @Test
        @DisplayName("Should move file and return 200")
        void shouldMoveFileAndReturn200() throws Exception {
            // given
            ResourceDto result = new ResourceDto(
                    "docs/work/file.txt",
                    "file.txt",
                    10L,
                    ResourceType.FILE
            );
            when(resourceService.move(1L, "docs/file.txt", "docs/work/file.txt"))
                    .thenReturn(result);

            // when & then
            mockMvc.perform(put(MOVE_PATH)
                            .param("from", "docs/file.txt")
                            .param("to", "docs/work/file.txt")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.path").value(result.path()))
                    .andExpect(jsonPath("$.name").value(result.name()))
                    .andExpect(jsonPath("$.size").value(result.size()))
                    .andExpect(jsonPath("$.type").value(result.type().toString()));
        }

        @Test
        @DisplayName("Should move file and return 200")
        void shouldMoveDirectoryAndReturn200() throws Exception {
            // given
            ResourceDto result = new ResourceDto(
                    "docs/work/",
                    "work",
                    null,
                    ResourceType.DIRECTORY
            );
            when(resourceService.move(1L, "docs/files/work/", "docs/work/"))
                    .thenReturn(result);

            // when & then
            mockMvc.perform(put(MOVE_PATH)
                            .param("from", "docs/files/work/")
                            .param("to", "docs/work/")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.path").value(result.path()))
                    .andExpect(jsonPath("$.name").value(result.name()))
                    .andExpect(jsonPath("$.size").value(result.size()))
                    .andExpect(jsonPath("$.type").value(result.type().toString()));
        }

        @Test
        @DisplayName("Should return 404 when from resource not found")
        void shouldReturn404_whenSourceNotFound() throws Exception {
            // given
            doThrow(new ResourceNotFoundException("Resource not found"))
                    .when(resourceService).move(1L, "docs/file.txt", "docs/work/file.txt");

            // when & then
            mockMvc.perform(put(MOVE_PATH)
                            .param("from", "docs/file.txt")
                            .param("to", "docs/work/file.txt")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 409 when to resource already exists")
        void shouldReturn409_whenToAlreadyExists() throws Exception {
            // given
            doThrow(new ResourceAlreadyExistsException("Resource already exists"))
                    .when(resourceService).move(1L, "docs/file.txt", "docs/work/file.txt");

            // when & then
            mockMvc.perform(put(MOVE_PATH)
                            .param("from", "docs/file.txt")
                            .param("to", "docs/work/file.txt")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should return 400 when move path to file")
        void shouldReturn400_whenMoveDirectoryToFile() throws Exception {
            // given
            doThrow(new InvalidMoveException("Unable to move path to file"))
                    .when(resourceService).move(1L, "docs/files/", "docs/files/file.txt");

            // when & then
            mockMvc.perform(put(MOVE_PATH)
                            .param("from", "docs/files/")
                            .param("to", "docs/files/file.txt")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when from and to are equals")
        void shouldReturn400_whenFromAndToEquals() throws Exception {
            mockMvc.perform(put(MOVE_PATH)
                            .param("from", "docs/file.txt")
                            .param("to", "docs/file.txt")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isBadRequest());
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "/docs/", "docs//work/", "docs/../work/", "docs/wo rk/", "docs/@work/"})
        @DisplayName("Should return 400 when invalid from path")
        void shouldReturn400_whenInvalidFromPath(String from) throws Exception {
            mockMvc.perform(put(MOVE_PATH)
                            .param("from", from)
                            .param("to", "docs/file.txt")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isBadRequest());
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "/docs/", "docs//work/", "docs/../work/", "docs/wo rk/", "docs/@work/"})
        @DisplayName("Should return 400 when invalid path")
        void shouldReturn400_whenInvalidToPath(String to) throws Exception {
            mockMvc.perform(put(MOVE_PATH)
                            .param("from", "docs/file.txt")
                            .param("to", to)
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 when user not authorized")
        void shouldReturn401_whenUserNotAuthorized() throws Exception {
            mockMvc.perform(put(MOVE_PATH)
                            .param("from", "docs/file.txt")
                            .param("to", "docs/files/file.txt"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class SearchTests {
        private static final String SEARCH_PATH = "/api/resource/search";

        @Test
        @DisplayName("Should return 200 and result when result is found")
        void shouldFindResults() throws Exception {
            // given
            ResourceDto result1 = new ResourceDto(
                    "docs/files/file1.txt",
                    "file1.txt",
                    10L,
                    ResourceType.FILE
            );
            ResourceDto result2 = new ResourceDto(
                    "docs/files/file2.txt",
                    "file2.txt",
                    10L,
                    ResourceType.FILE
            );
            when(resourceService.search(1L, "file"))
                    .thenReturn(List.of(result1, result2));

            mockMvc.perform(get(SEARCH_PATH)
                            .param("query", "file")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].path").value(result1.path()))
                    .andExpect(jsonPath("$[0].name").value(result1.name()))
                    .andExpect(jsonPath("$[0].size").value(result1.size()))
                    .andExpect(jsonPath("$[0].type").value(result1.type().toString()))
                    .andExpect(jsonPath("$[1].path").value(result2.path()))
                    .andExpect(jsonPath("$[1].name").value(result2.name()))
                    .andExpect(jsonPath("$[1].size").value(result2.size()))
                    .andExpect(jsonPath("$[1].type").value(result2.type().toString()));
        }

        @Test
        @DisplayName("Should return 200 and empty list when results not found")
        void shouldReturnEmptyList_whenNotFound() throws Exception {
            // given
            when(resourceService.search(1L, "file"))
                    .thenReturn(List.of());

            mockMvc.perform(get(SEARCH_PATH)
                            .param("query", "file")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "/docs/", "docs//work/", "docs/../work/", "docs/wo rk/", "docs/@work/"})
        @DisplayName("Should return 400 when invalid path")
        void shouldReturn400_whenInvalidQuery(String value) throws Exception {
            mockMvc.perform(get(SEARCH_PATH)
                            .param("query", value)
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 when user not authorized")
        void shouldReturn401_whenUserNotAuthorized() throws Exception {
            mockMvc.perform(get(SEARCH_PATH)
                            .param("from", "docs/file.txt")
                            .param("to", "docs/files/file.txt"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class UploadTests {
        private static final String UPLOAD_PATH = "/api/resource";

        @Test
        @DisplayName("Should upload file and return 201")
        void shouldUploadFileAndReturn201() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "files", "file.txt", "text/plain", "content".getBytes());
            FileData fileData = FileData.builder()
                    .filename("file.txt").directory("docs").size(7L)
                    .contentType("text/plain").inputStreamSupplier(() -> new ByteArrayInputStream("content".getBytes()))
                    .build();
            ResourceDto dto = new ResourceDto("docs/", "file.txt", 7L, ResourceType.FILE);

            when(multipartFileDataParser.parse(any(MultipartFile.class), eq("docs")))
                    .thenReturn(fileData);
            when(resourceService.upload(eq(1L), anyList()))
                    .thenReturn(List.of(dto));

            mockMvc.perform(multipart(UPLOAD_PATH)
                            .file(file)
                            .param("path", "docs/")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name").value("file.txt"))
                    .andExpect(jsonPath("$[0].type").value("FILE"));
        }

        @Test
        @DisplayName("Should upload multiple files and return 201")
        void shouldUploadMultipleFilesAndReturn201() throws Exception {
            MockMultipartFile file1 = new MockMultipartFile(
                    "files", "a.txt", "text/plain", "aaa".getBytes());
            MockMultipartFile file2 = new MockMultipartFile(
                    "files", "b.txt", "text/plain", "bbb".getBytes());
            ResourceDto dto1 = new ResourceDto("docs/", "a.txt", 3L, ResourceType.FILE);
            ResourceDto dto2 = new ResourceDto("docs/", "b.txt", 3L, ResourceType.FILE);

            when(multipartFileDataParser.parse(any(MultipartFile.class), eq("docs")))
                    .thenReturn(mock(FileData.class));
            when(resourceService.upload(eq(1L), anyList()))
                    .thenReturn(List.of(dto1, dto2));

            mockMvc.perform(multipart(UPLOAD_PATH)
                            .file(file1)
                            .file(file2)
                            .param("path", "docs/")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("Should return 409 when file already exists")
        void shouldReturn409_whenFileAlreadyExists() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "files", "file.txt", "text/plain", "content".getBytes());

            when(multipartFileDataParser.parse(any(MultipartFile.class), eq("docs")))
                    .thenReturn(mock(FileData.class));
            when(resourceService.upload(eq(1L), anyList()))
                    .thenThrow(new ResourceAlreadyExistsException("File already exists: docs/file.txt"));

            mockMvc.perform(multipart(UPLOAD_PATH)
                            .file(file)
                            .param("path", "docs/")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should return 400 when invalid path path")
        void shouldReturn400_whenInvalidDirectoryPath() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "files", "file.txt", "text/plain", "content".getBytes());

            mockMvc.perform(multipart(UPLOAD_PATH)
                            .file(file)
                            .param("path", "docs/@invalid/")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should upload to root when path is empty")
        void shouldUploadToRoot_whenDirectoryIsEmpty() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "files", "file.txt", "text/plain", "content".getBytes());
            ResourceDto dto = new ResourceDto("", "file.txt", 7L, ResourceType.FILE);

            when(multipartFileDataParser.parse(any(MultipartFile.class), eq("")))
                    .thenReturn(mock(FileData.class));
            when(resourceService.upload(eq(1L), anyList()))
                    .thenReturn(List.of(dto));

            mockMvc.perform(multipart(UPLOAD_PATH)
                            .file(file)
                            .param("path", "")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should return 400 when no files provided")
        void shouldReturn400_whenNoFilesProvided() throws Exception {
            mockMvc.perform(multipart(UPLOAD_PATH)
                            .param("path", "docs")
                            .with(user(new CustomUserDetails(1L, "user", "pass"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 when user not authorized")
        void shouldReturn401_whenUserNotAuthorized() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "files", "file.txt", "text/plain", "content".getBytes());

            mockMvc.perform(multipart(UPLOAD_PATH)
                            .file(file)
                            .param("path", "docs"))
                    .andExpect(status().isUnauthorized());
        }
    }

}
