package com.waynehays.cloudfilestorage.unit.service.file.downloader;

import com.waynehays.cloudfilestorage.constant.Constants;
import com.waynehays.cloudfilestorage.dto.files.ResourcePath;
import com.waynehays.cloudfilestorage.dto.files.response.FileDownloadDto;
import com.waynehays.cloudfilestorage.dto.files.response.ResourceType;
import com.waynehays.cloudfilestorage.entity.FileInfo;
import com.waynehays.cloudfilestorage.entity.User;
import com.waynehays.cloudfilestorage.exception.FileNotFoundException;
import com.waynehays.cloudfilestorage.exception.FileStorageException;
import com.waynehays.cloudfilestorage.filestorage.MinioFileStorage;
import com.waynehays.cloudfilestorage.parser.resourcepathparser.ResourcePathParserImpl;
import com.waynehays.cloudfilestorage.service.file.downloader.FileDownloaderImpl;
import com.waynehays.cloudfilestorage.service.fileinfo.FileInfoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileDownloaderImplTest {
    private static final Long USER_ID = 1L;
    private static final String DIRECTORY = "docs";
    private static final String FILENAME = "file";
    private static final String EXTENSION = ".txt";
    private static final String STORAGE_KEY = "123/docs/uuid.txt";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final long FILE_SIZE = 1024L;
    private static final String CONTENT_TYPE = "text/plain";

    @Mock
    private MinioFileStorage fileStorage;

    @Mock
    private FileInfoServiceImpl fileInfoService;

    @Mock
    private ResourcePathParserImpl queryPathParser;

    @InjectMocks
    private FileDownloaderImpl fileDownloader;

    private User mockUser;
    private InputStream mockInputStream;

    @BeforeEach
    void setUp() {
        mockUser = createMockUser();
        FileInfo mockFileInfo = createMockFileInfo();
        mockInputStream = new ByteArrayInputStream(new byte[]{1, 2, 3});
        lenient().when(fileInfoService.find(anyLong(), anyString(), anyString()))
                .thenReturn(mockFileInfo);
    }

    @Test
    @DisplayName("Should download file successfully")
    void shouldDownloadFileSuccessfully() {
        // given
        String path = DIRECTORY + Constants.PATH_SEPARATOR + FILENAME;
        ResourcePath resourcePath = new ResourcePath(DIRECTORY, FILENAME, ResourceType.FILE);

        when(queryPathParser.parse(path)).thenReturn(resourcePath);
        when(fileStorage.get(STORAGE_KEY)).thenReturn(Optional.of(mockInputStream));

        // when
        FileDownloadDto result = fileDownloader.download(USER_ID, path);

        // then
        assertThat(result).isNotNull();
        assertThat(result.inputStream()).isEqualTo(mockInputStream);
        assertThat(result.size()).isEqualTo(FILE_SIZE);

        verify(queryPathParser).parse(path);
        verify(fileInfoService).find(USER_ID, DIRECTORY, FILENAME);
        verify(fileStorage).get(STORAGE_KEY);
    }

//    @Test
//    @DisplayName("Should throw UnsupportedOperationException for directory download")
//    void shouldThrowExceptionForDirectoryDownload() {
//        // given
//        String path = DIRECTORY + Constants.PATH_SEPARATOR;
//        ParsedPath parsedPath = new ParsedPath(DIRECTORY, null, ResourceType.DIRECTORY);
//
//        when(queryPathParser.parse(path)).thenReturn(parsedPath);
//
//        // when & then
//        assertThatThrownBy(() -> fileDownloader.download(USER_ID, path))
//                .isInstanceOf(UnsupportedOperationException.class)
//                .hasMessageContaining("Directory download not implemented");
//    }

    @Test
    @DisplayName("Should throw FileNotFoundException when file not in database")
    void shouldThrowFileNotFoundExceptionWhenFileNotInDatabase() {
        // given
        String path = DIRECTORY + Constants.PATH_SEPARATOR + FILENAME;
        ResourcePath resourcePath = new ResourcePath(DIRECTORY, FILENAME, ResourceType.FILE);

        when(queryPathParser.parse(path)).thenReturn(resourcePath);

        // when & then
        assertThatThrownBy(() -> fileDownloader.download(USER_ID, path))
                .isInstanceOf(FileNotFoundException.class)
                .hasMessageContaining("File not found");
    }

    @Test
    @DisplayName("Should throw FileNotFoundException when file not in storage")
    void shouldThrowFileNotFoundExceptionWhenFileNotInStorage() {
        // given
        String path = DIRECTORY + Constants.PATH_SEPARATOR + FILENAME;
        ResourcePath resourcePath = new ResourcePath(DIRECTORY, FILENAME, ResourceType.FILE);

        when(queryPathParser.parse(path)).thenReturn(resourcePath);
        when(fileStorage.get(STORAGE_KEY)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> fileDownloader.download(USER_ID, path))
                .isInstanceOf(FileNotFoundException.class)
                .hasMessageContaining("File not found in storage");
    }

    @Test
    @DisplayName("Should throw FileStorageException on storage error")
    void shouldThrowFileStorageExceptionOnStorageError() {
        // given
        String path = DIRECTORY + Constants.PATH_SEPARATOR + FILENAME;
        ResourcePath resourcePath = new ResourcePath(DIRECTORY, FILENAME, ResourceType.FILE);

        when(queryPathParser.parse(path)).thenReturn(resourcePath);
        when(fileStorage.get(STORAGE_KEY)).thenThrow(new FileStorageException("Failed to get object with key: " + STORAGE_KEY));

        // when & then
        assertThatThrownBy(() -> fileDownloader.download(USER_ID, path))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("Failed to get object with key: " + STORAGE_KEY);
    }

    private User createMockUser() {
        return User.builder()
                .id(USER_ID)
                .username(USERNAME)
                .password(PASSWORD)
                .build();
    }

    private FileInfo createMockFileInfo() {
        return FileInfo.builder()
                .id(1L)
                .directory(DIRECTORY)
                .name(FILENAME)
                .storageKey(STORAGE_KEY)
                .size(FILE_SIZE)
                .contentType(CONTENT_TYPE)
                .user(mockUser)
                .build();
    }
}
