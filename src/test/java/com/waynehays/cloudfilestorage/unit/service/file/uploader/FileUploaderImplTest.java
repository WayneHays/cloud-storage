package com.waynehays.cloudfilestorage.unit.service.file.uploader;

import com.waynehays.cloudfilestorage.constant.Constants;
import com.waynehays.cloudfilestorage.dto.files.FileData;
import com.waynehays.cloudfilestorage.dto.files.response.ResourceDto;
import com.waynehays.cloudfilestorage.dto.files.response.ResourceType;
import com.waynehays.cloudfilestorage.entity.FileInfo;
import com.waynehays.cloudfilestorage.entity.User;
import com.waynehays.cloudfilestorage.exception.FileStorageException;
import com.waynehays.cloudfilestorage.exception.InvalidPathException;
import com.waynehays.cloudfilestorage.filestorage.MinioFileStorage;
import com.waynehays.cloudfilestorage.mapper.FileInfoMapperImpl;
import com.waynehays.cloudfilestorage.parser.multipartfiledataparser.MultipartFileDataParserImpl;
import com.waynehays.cloudfilestorage.service.file.uploader.FileUploaderImpl;
import com.waynehays.cloudfilestorage.service.fileinfo.FileInfoServiceImpl;
import com.waynehays.cloudfilestorage.service.keygenerator.StorageKeyGeneratorImpl;
import com.waynehays.cloudfilestorage.validator.PathValidatorImpl;
import org.apache.tomcat.util.http.fileupload.InvalidFileNameException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileUploaderImplTest {
    private static final Long USER_ID = 1L;
    private static final String DIRECTORY = "docs";
    private static final String NAME = "file";
    private static final String EXTENSION = "txt";
    private static final String FILENAME = NAME + Constants.EXTENSION_SEPARATOR + EXTENSION;
    private static final String STORAGE_KEY = "1/docs/uuid-123";
    private static final String CONTENT = "content";
    private static final String CONTENT_TYPE = "text/plain";
    private static final long FILE_SIZE = 1024L;

    @Mock
    private PathValidatorImpl pathValidator;

    @Mock
    private MultipartFileDataParserImpl parser;

    @Mock
    private MinioFileStorage fileStorage;

    @Mock
    private FileInfoServiceImpl fileInfoService;

    @Mock
    private StorageKeyGeneratorImpl storageKeyGenerator;

    @Mock
    private FileInfoMapperImpl fileInfoMapper;

    @InjectMocks
    private FileUploaderImpl fileUploader;

    private MultipartFile multipartFile;
    private FileData fileData;
    private FileInfo fileInfo;
    private ResourceDto resourceDto;

    @BeforeEach
    void setUp() {
        multipartFile = new MockMultipartFile(
                NAME, FILENAME, CONTENT_TYPE, CONTENT.getBytes());
        fileData = new FileData(
                FILENAME, FILENAME, DIRECTORY, EXTENSION,
                FILE_SIZE, CONTENT_TYPE, new ByteArrayInputStream(CONTENT.getBytes())
        );
        fileInfo = createFileInfo();
        resourceDto = new ResourceDto(FILENAME, DIRECTORY, FILE_SIZE, ResourceType.FILE);
    }

    @Test
    @DisplayName("Should upload file with directory successfully")
    void shouldUploadFileWithDirectory() {
        // given
        setupSuccessfulUploadTo(DIRECTORY);

        // when
        ResourceDto result = fileUploader.uploadFile(USER_ID, DIRECTORY, multipartFile);

        // then
        assertThat(result).isEqualTo(resourceDto);
        verifySuccessfulUploadInteractions(DIRECTORY);
    }

    @Test
    @DisplayName("Should upload file without directory")
    void shouldUploadFileWithoutDirectory() {
        // given
        setupSuccessfulUploadTo(null);

        // when
        ResourceDto result = fileUploader.uploadFile(USER_ID, null, multipartFile);

        // then
        assertThat(result).isEqualTo(resourceDto);
    }

    @Test
    @DisplayName("Should throw when filename is invalid")
    void shouldThrowWhenFilenameInvalid() {
        // given
        doThrow(new InvalidFileNameException("Invalid", FILENAME))
                .when(pathValidator).validateUploadPath(FILENAME, DIRECTORY);

        // when & then
        assertThatThrownBy(() -> fileUploader.uploadFile(USER_ID, DIRECTORY, multipartFile))
                .isInstanceOf(InvalidFileNameException.class);

        verifyNoInteractions(parser, fileStorage, fileInfoService, storageKeyGenerator);
    }

    @Test
    @DisplayName("Should throw when directory path is invalid")
    void shouldThrowWhenDirectoryInvalid() {
        // given
        doThrow(new InvalidPathException("Invalid"))
                .when(pathValidator).validateUploadPath(FILENAME, DIRECTORY);

        // when & then
        assertThatThrownBy(() -> fileUploader.uploadFile(USER_ID, DIRECTORY, multipartFile))
                .isInstanceOf(InvalidPathException.class);

        verifyNoInteractions(parser, fileStorage, fileInfoService, storageKeyGenerator);
    }

    @Test
    @DisplayName("Should rollback database when storage save fails")
    void shouldRollbackDatabaseWhenStorageFails() {
        setupSuccessfulSaveTo(DIRECTORY);
        doThrow(new FileStorageException("Storage failed"))
                .when(fileStorage).put(any(), eq(STORAGE_KEY), eq(FILE_SIZE), eq(CONTENT_TYPE));

        assertThatThrownBy(() -> fileUploader.uploadFile(USER_ID, DIRECTORY, multipartFile))
                .isInstanceOf(FileStorageException.class);

        verify(fileInfoService).deleteFile(USER_ID, DIRECTORY, FILENAME);
    }

    @Test
    @DisplayName("Should propagate storage exception when rollback also fails")
    void shouldPropagateStorageExceptionWhenRollbackFails() {
        setupSuccessfulSaveTo(DIRECTORY);
        doThrow(new FileStorageException("Storage failed"))
                .when(fileStorage).put(any(), eq(STORAGE_KEY), eq(FILE_SIZE), eq(CONTENT_TYPE));
        doThrow(new RuntimeException("Rollback failed"))
                .when(fileInfoService).deleteFile(USER_ID, DIRECTORY, FILENAME);

        assertThatThrownBy(() -> fileUploader.uploadFile(USER_ID, DIRECTORY, multipartFile))
                .isInstanceOf(FileStorageException.class)
                .hasMessage("Storage failed");
    }

    @Test
    @DisplayName("Should pass generated storage key to both storage and service")
    void shouldPassStorageKeyToStorageAndService() {
        // given
        setupSuccessfulUploadTo(DIRECTORY);

        // when
        fileUploader.uploadFile(USER_ID, DIRECTORY, multipartFile);

        // then
        verify(storageKeyGenerator).generate(USER_ID, DIRECTORY, FILENAME);
        verify(fileInfoService).save(USER_ID, fileData, STORAGE_KEY);
        verify(fileStorage).put(any(), eq(STORAGE_KEY), eq(FILE_SIZE), eq(CONTENT_TYPE));
    }

    @Test
    @DisplayName("Should pass parsed file data to service and storage")
    void shouldPassFileDataToServiceAndStorage() {
        // given
        setupSuccessfulUploadTo(DIRECTORY);

        // when
        fileUploader.uploadFile(USER_ID, DIRECTORY, multipartFile);

        // then
        verify(parser).parse(multipartFile, DIRECTORY);
        verify(fileInfoService).save(USER_ID, fileData, STORAGE_KEY);
        verify(fileStorage).put(
                eq(fileData.inputStream()),
                eq(STORAGE_KEY),
                eq(fileData.size()),
                eq(fileData.contentType())
        );
    }

    private void setupSuccessfulUploadTo(String directory) {
        setupSuccessfulSaveTo(directory);
        when(fileInfoMapper.toResourceDto(fileInfo)).thenReturn(resourceDto);
    }

    private void setupSuccessfulSaveTo(String directory) {
        when(parser.parse(multipartFile, directory)).thenReturn(fileData);
        when(storageKeyGenerator.generate(USER_ID, DIRECTORY, FILENAME))
                .thenReturn(STORAGE_KEY);
        when(fileInfoService.save(USER_ID, fileData, STORAGE_KEY)).thenReturn(fileInfo);
    }

    private void verifySuccessfulUploadInteractions(String directory) {
        InOrder inOrder = inOrder(pathValidator, parser, storageKeyGenerator, fileInfoService, fileStorage);
        inOrder.verify(pathValidator).validateUploadPath(FILENAME, directory);
        inOrder.verify(parser).parse(multipartFile, directory);
        inOrder.verify(storageKeyGenerator).generate(USER_ID, DIRECTORY, FILENAME);
        inOrder.verify(fileInfoService).save(USER_ID, fileData, STORAGE_KEY);
        inOrder.verify(fileStorage).put(any(), eq(STORAGE_KEY), eq(FILE_SIZE), eq(CONTENT_TYPE));
    }

    private FileInfo createFileInfo() {
        User user = new User();
        user.setId(USER_ID);

        return FileInfo.builder()
                .directory(DIRECTORY)
                .name(FILENAME)
                .storageKey(STORAGE_KEY)
                .size(FILE_SIZE)
                .contentType(CONTENT_TYPE)
                .user(user)
                .build();
    }
}
