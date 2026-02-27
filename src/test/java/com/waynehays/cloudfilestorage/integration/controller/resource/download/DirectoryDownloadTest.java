package com.waynehays.cloudfilestorage.integration.controller.resource.download;

import com.waynehays.cloudfilestorage.constant.Constants;
import com.waynehays.cloudfilestorage.integration.base.AbstractControllerIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DirectoryDownloadTest extends AbstractControllerIntegrationTest {
    private static final String DOWNLOAD_URL = "/api/resource/download";
    private static final String PARAM_PATH = "path";

    private static final String FOLDER_1 = "docs";
    private static final String FOLDER_2 = "work";
    private static final String FOLDER_3 = "task";

    private static final String FILE_1 = "file1.txt";
    private static final String FILE_2 = "file2.txt";
    private static final String FILE_3 = "file3.txt";

    private static final String CONTENT_1 = "text1";
    private static final String CONTENT_2 = "text2";
    private static final String CONTENT_3 = "text3";

    private static final String DIRECTORY_1 = FOLDER_1;
    private static final String DIRECTORY_2 = join(DIRECTORY_1, FOLDER_2);
    private static final String DIRECTORY_3 = join(DIRECTORY_2, FOLDER_3);

    private static final String ZIP_ENTRY_1 = FILE_1;
    private static final String ZIP_ENTRY_2 = join(FOLDER_2, FILE_2);
    private static final String ZIP_ENTRY_3 = join(FOLDER_2, FOLDER_3, FILE_3);

    private static final String ROOT_ZIP_ENTRY_1 = join(DIRECTORY_1, FILE_1);
    private static final String ROOT_ZIP_ENTRY_2 = join(DIRECTORY_2, FILE_2);
    private static final String ROOT_ZIP_ENTRY_3 = join(DIRECTORY_3, FILE_3);

    @Test
    @DisplayName("Should download directory with single file")
    void shouldDownloadDirectoryWithFile() throws Exception {
        // given
        uploadFile(FILE_1, CONTENT_1, DIRECTORY_1);

        // when
        Map<String, String> zip = downloadDirectoryAsZip(DIRECTORY_1);

        // then
        assertThat(zip).hasSize(1)
                .containsEntry(FILE_1, CONTENT_1);
    }

    @Test
    @DisplayName("Should download directory with multiple files")
    void shouldDownloadDirectoryWith2Files() throws Exception {
        // given
        uploadFile(FILE_1, CONTENT_1, DIRECTORY_1);
        uploadFile(FILE_2, CONTENT_2, DIRECTORY_1);

        // when
        Map<String, String> zip = downloadDirectoryAsZip(DIRECTORY_1);

        // then
        assertThat(zip).hasSize(2)
                .containsEntry(FILE_1, CONTENT_1)
                .containsEntry(FILE_2, CONTENT_2);
    }

    @Test
    @DisplayName("Should download directory with nested directories")
    void shouldCorrectlyDownloadDirectoryWithNestedDirectories() throws Exception {
        // given
        uploadFile(FILE_1, CONTENT_1, DIRECTORY_1);
        uploadFile(FILE_2, CONTENT_2, DIRECTORY_2);
        uploadFile(FILE_3, CONTENT_3, DIRECTORY_3);

        // when
        Map<String, String> zip = downloadDirectoryAsZip(DIRECTORY_1);

        // then
        assertThat(zip).hasSize(3)
                .containsEntry(ZIP_ENTRY_1, CONTENT_1)
                .containsEntry(ZIP_ENTRY_2, CONTENT_2)
                .containsEntry(ZIP_ENTRY_3, CONTENT_3);
    }

    @Test
    @DisplayName("Should contain relative paths in zip")
    void shouldContainRelativePaths() throws Exception {
        // given
        uploadFile(FILE_1, CONTENT_1, DIRECTORY_1);
        uploadFile(FILE_2, CONTENT_2, DIRECTORY_2);
        uploadFile(FILE_3, CONTENT_3, DIRECTORY_3);

        // when
        Map<String, String> zip = downloadDirectoryAsZip(DIRECTORY_1);

        // then
        assertThat(zip.keySet()).isNotEmpty().allSatisfy(
                path -> assertThat(path).doesNotStartWith(DIRECTORY_1 + Constants.PATH_SEPARATOR)
        );
    }

    @Test
    @DisplayName("Should contain correct Content-Disposition")
    void shouldContainCorrectContentDisposition() throws Exception {
        // given
        uploadFile(FILE_1, CONTENT_1, DIRECTORY_1);

        // when & then
        mockMvc.perform(get(DOWNLOAD_URL)
                        .param(PARAM_PATH, DIRECTORY_1 + Constants.PATH_SEPARATOR)
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"%s.zip\"".formatted(DIRECTORY_1)));
    }

    @Test
    @DisplayName("Should download all user files when download root directory")
    void shouldDownloadAllFiles() throws Exception {
        // given
        uploadFile(FILE_1, CONTENT_1, DIRECTORY_1);
        uploadFile(FILE_2, CONTENT_2, DIRECTORY_2);
        uploadFile(FILE_3, CONTENT_3, DIRECTORY_3);

        // when
        Map<String, String> zip = downloadDirectoryAsZip("");

        // then
        assertThat(zip).hasSize(3)
                .containsEntry(ROOT_ZIP_ENTRY_1, CONTENT_1)
                .containsEntry(ROOT_ZIP_ENTRY_2, CONTENT_2)
                .containsEntry(ROOT_ZIP_ENTRY_3, CONTENT_3);
    }

    private Map<String, String> downloadDirectoryAsZip(String directory) throws Exception {
        MvcResult result = mockMvc.perform(get(DOWNLOAD_URL)
                        .param(PARAM_PATH, directory + Constants.PATH_SEPARATOR)
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andReturn();

        return extractZipContent(result.getResponse().getContentAsByteArray());
    }

    private Map<String, String> extractZipContent(byte[] zipBytes) throws IOException {
        Map<String, String> contents = new HashMap<>();

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                String content = new String(zis.readAllBytes());
                contents.put(entry.getName(), content);
                zis.closeEntry();
            }
        }
        return contents;
    }
}
