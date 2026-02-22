package com.waynehays.cloudfilestorage.unit.service;

import com.waynehays.cloudfilestorage.dto.files.FileData;
import com.waynehays.cloudfilestorage.dto.files.response.ResourceDto;
import com.waynehays.cloudfilestorage.dto.files.response.ResourceType;
import com.waynehays.cloudfilestorage.entity.FileInfo;
import com.waynehays.cloudfilestorage.entity.User;
import com.waynehays.cloudfilestorage.exception.EmptyFileException;
import com.waynehays.cloudfilestorage.exception.FileAlreadyExistsException;
import com.waynehays.cloudfilestorage.extractor.MultipartFileDataExtractor;
import com.waynehays.cloudfilestorage.filestorage.FileStorage;
import com.waynehays.cloudfilestorage.mapper.FileInfoMapper;
import com.waynehays.cloudfilestorage.repository.FileInfoRepository;
import com.waynehays.cloudfilestorage.repository.UserRepository;
import com.waynehays.cloudfilestorage.service.FileServiceImpl;
import com.waynehays.cloudfilestorage.validator.UploadPathValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {
    private static final Long USER_ID = 123L;
    private static final String USERNAME = "test-user";

    private static final String FILENAME = "file.txt";
    private static final String ORIGINAL_FILENAME = "original.txt";
    private static final String README_FILENAME = "README";
    private static final String EXTENSION = "txt";
    private static final String EMPTY_EXTENSION = "";
    private static final String CONTENT_TYPE = "text/plain";
    private static final long SIZE = 1024L;

    private static final String DIRECTORY = "documents";
    private static final String EMPTY_DIRECTORY = "";

    private static final String STORAGE_KEY = "123/documents/uuid.txt";
    private static final String SEPARATOR = "/";
    private static final String DOT = ".";

    private static final String MSG_EMPTY_FILE = "File to upload is empty";
    private static final String MSG_FILE_ALREADY_EXISTS = "File already exists";
    private static final String MSG_DUPLICATE_KEY = "Duplicate key";

    @Mock
    private FileStorage fileStorage;

    @Mock
    private FileInfoRepository fileInfoRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UploadPathValidator uploadPathValidator;

    @Mock
    private MultipartFileDataExtractor multipartFileDataExtractor;

    @Mock
    private FileInfoMapper fileInfoMapper;

    @InjectMocks
    private FileServiceImpl fileService;

    private MultipartFile mockFile;
    private User mockUser;
    private InputStream inputStream;
    private FileData mockFileData;
    private FileInfo mockFileInfo;
    private ResourceDto mockResourceDto;

    @BeforeEach
    void setUp() {
        mockFile = mock(MultipartFile.class);

        mockUser = User.builder()
                .id(USER_ID)
                .username(USERNAME)
                .build();

        inputStream = new ByteArrayInputStream(new byte[0]);

        mockFileData = FileData.builder()
                .originalFilename(ORIGINAL_FILENAME)
                .filename(FILENAME)
                .directory(DIRECTORY)
                .extension(EXTENSION)
                .size(SIZE)
                .contentType(CONTENT_TYPE)
                .inputStream(inputStream)
                .build();

        mockFileInfo = FileInfo.builder()
                .id(1L)
                .directory(DIRECTORY)
                .name(FILENAME)
                .storageKey(STORAGE_KEY)
                .size(SIZE)
                .contentType(CONTENT_TYPE)
                .user(mockUser)
                .build();

        mockResourceDto = new ResourceDto(
                DIRECTORY,
                FILENAME,
                SIZE,
                ResourceType.FILE
        );

        lenient().when(mockFile.getOriginalFilename()).thenReturn(FILENAME);
        when(mockFile.isEmpty()).thenReturn(false);
    }

    @Nested
    @DisplayName("UploadFile - Success cases")
    class UploadFileSuccessTests {

        @BeforeEach
        void setUp() {
            when(userRepository.getReferenceById(USER_ID)).thenReturn(mockUser);
            lenient().when(multipartFileDataExtractor.extract(mockFile, DIRECTORY)).thenReturn(mockFileData);
            when(fileInfoRepository.save(any(FileInfo.class))).thenReturn(mockFileInfo);
            when(fileInfoMapper.toResourceDto(mockFileInfo)).thenReturn(mockResourceDto);
        }

        @Test
        @DisplayName("Should successfully upload file")
        void shouldSuccessfullyUploadFile() {
            // when
            ResourceDto result = fileService.uploadFile(USER_ID, DIRECTORY, mockFile);

            // then
            assertThat(result).isNotNull().isEqualTo(mockResourceDto);
            verify(uploadPathValidator).validate(anyString(), eq(DIRECTORY));
            verify(multipartFileDataExtractor).extract(mockFile, DIRECTORY);
            verify(userRepository).getReferenceById(USER_ID);
            verify(fileStorage).put(
                    any(InputStream.class),
                    anyString(),
                    eq(SIZE),
                    eq(CONTENT_TYPE));
            verify(fileInfoRepository).save(any(FileInfo.class));
            verify(fileInfoMapper).toResourceDto(mockFileInfo);
        }

        @Test
        @DisplayName("Should generate storage key with correct format")
        void shouldGenerateStorageKeyWithCorrectFormat() {
            // given
            ArgumentCaptor<String> storageKeyCaptor = ArgumentCaptor.forClass(String.class);

            // when
            fileService.uploadFile(USER_ID, DIRECTORY, mockFile);

            // then
            verify(fileStorage).put(
                    any(InputStream.class),
                    storageKeyCaptor.capture(),
                    eq(SIZE),
                    eq(CONTENT_TYPE)
            );

            String storageKey = storageKeyCaptor.getValue();
            assertThat(storageKey)
                    .startsWith(USER_ID + SEPARATOR + DIRECTORY + SEPARATOR)
                    .endsWith(DOT + EXTENSION);
        }

        @Test
        @DisplayName("Should generate storage key without directory when empty")
        void shouldGenerateStorageKeyWithoutDirectory() {
            // given
            FileData fileDataNoDir = FileData.builder()
                    .originalFilename(FILENAME)
                    .filename(FILENAME)
                    .directory(EMPTY_DIRECTORY)
                    .extension(EXTENSION)
                    .size(SIZE)
                    .contentType(CONTENT_TYPE)
                    .inputStream(inputStream)
                    .build();

            when(multipartFileDataExtractor.extract(mockFile, EMPTY_DIRECTORY)).thenReturn(fileDataNoDir);

            ArgumentCaptor<String> storageKeyCaptor = ArgumentCaptor.forClass(String.class);

            // when
            fileService.uploadFile(USER_ID, EMPTY_DIRECTORY, mockFile);

            // then
            verify(fileStorage).put(
                    any(InputStream.class),
                    storageKeyCaptor.capture(),
                    eq(SIZE),
                    eq(CONTENT_TYPE)
            );

            String storageKey = storageKeyCaptor.getValue();
            assertThat(storageKey)
                    .startsWith(USER_ID + SEPARATOR)
                    .doesNotContain(SEPARATOR + SEPARATOR)
                    .endsWith(DOT + EXTENSION);
        }

        @Test
        @DisplayName("Should generate storage key without extension when empty")
        void shouldGenerateStorageKeyWithoutExtension() {
            // given
            FileData fileDataNoExt = FileData.builder()
                    .originalFilename(README_FILENAME)
                    .filename(README_FILENAME)
                    .directory(DIRECTORY)
                    .extension(EMPTY_EXTENSION)
                    .size(SIZE)
                    .contentType(CONTENT_TYPE)
                    .inputStream(inputStream)
                    .build();

            when(multipartFileDataExtractor.extract(mockFile, DIRECTORY)).thenReturn(fileDataNoExt);
            ArgumentCaptor<String> storageKeyCaptor = ArgumentCaptor.forClass(String.class);

            // when
            fileService.uploadFile(USER_ID, DIRECTORY, mockFile);

            // then
            verify(fileStorage).put(
                    any(InputStream.class),
                    storageKeyCaptor.capture(),
                    eq(SIZE),
                    eq(CONTENT_TYPE)
            );

            String storageKey = storageKeyCaptor.getValue();
            assertThat(storageKey)
                    .startsWith(USER_ID + SEPARATOR + DIRECTORY + SEPARATOR)
                    .doesNotEndWith(DOT);
        }

        @Test
        @DisplayName("Should use getReferenceById for user")
        void shouldUseGetReferenceByIdForUser() {
            // when
            fileService.uploadFile(USER_ID, DIRECTORY, mockFile);

            // then
            verify(userRepository).getReferenceById(USER_ID);
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should call validator with correct params")
        void shouldCallValidatorWithCorrectParams() {
            // when
            fileService.uploadFile(USER_ID, DIRECTORY, mockFile);

            // then
            verify(uploadPathValidator).validate(mockFile.getOriginalFilename(), DIRECTORY);
        }

        @Test
        @DisplayName("Should call handler with correct params")
        void shouldCallExtractorWithCorrectParams() {
            // when
            fileService.uploadFile(USER_ID, DIRECTORY, mockFile);

            // then
            verify(multipartFileDataExtractor).extract(mockFile, DIRECTORY);
        }

        @Test
        @DisplayName("Should save to MinIO before database")
        void shouldSaveToMinioBeforeDatabase() {
            // when
            fileService.uploadFile(USER_ID, DIRECTORY, mockFile);

            // then
            InOrder inOrder = inOrder(fileStorage, fileInfoRepository);
            inOrder.verify(fileStorage).put(
                    any(InputStream.class),
                    anyString(),
                    eq(SIZE),
                    eq(CONTENT_TYPE));
            inOrder.verify(fileInfoRepository).save(any(FileInfo.class));
        }

        @Test
        @DisplayName("Should create FileInfo with correct data")
        void shouldCreateFileInfoWithCorrectData() {
            // given
            ArgumentCaptor<FileInfo> fileInfoCaptor = ArgumentCaptor.forClass(FileInfo.class);

            // when
            fileService.uploadFile(USER_ID, DIRECTORY, mockFile);

            // then
            verify(fileInfoRepository).save(fileInfoCaptor.capture());

            FileInfo capturedFileInfo = fileInfoCaptor.getValue();
            assertThat(capturedFileInfo.getDirectory()).isEqualTo(DIRECTORY);
            assertThat(capturedFileInfo.getName()).isEqualTo(FILENAME);
            assertThat(capturedFileInfo.getSize()).isEqualTo(SIZE);
            assertThat(capturedFileInfo.getContentType()).isEqualTo(CONTENT_TYPE);
            assertThat(capturedFileInfo.getUser()).isEqualTo(mockUser);
        }
    }

    @Nested
    @DisplayName("uploadFile - Validation failures")
    class UploadFileValidationTests {

        @Test
        @DisplayName("Should throw EmptyFileException when file is empty")
        void shouldThrowEmptyFileExceptionWhenFileIsEmpty() {
            // given
            when(mockFile.isEmpty()).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> fileService.uploadFile(USER_ID, DIRECTORY, mockFile))
                    .isInstanceOf(EmptyFileException.class)
                    .hasMessageContaining(MSG_EMPTY_FILE);
        }

        @Test
        @DisplayName("Should not call validator when file is empty")
        void shouldNotCallValidatorWhenFileIsEmpty() {
            // given
            when(mockFile.isEmpty()).thenReturn(true);

            // when
            try {
                fileService.uploadFile(USER_ID, DIRECTORY, mockFile);
            } catch (EmptyFileException ignored) {
            }

            // Assert
            verify(uploadPathValidator, never()).validate(anyString(), anyString());
            verify(multipartFileDataExtractor, never()).extract(any(), anyString());
            verify(fileStorage, never()).put(any(), anyString(), anyLong(), anyString());
        }
    }

    @Nested
    @DisplayName("uploadFile - Duplicate file handling")
    class UploadFileDuplicateTests {

        @BeforeEach
        void setUp() {
            when(userRepository.getReferenceById(USER_ID)).thenReturn(mockUser);
            when(multipartFileDataExtractor.extract(mockFile, DIRECTORY)).thenReturn(mockFileData);
        }

        @Test
        @DisplayName("Should throw FileAlreadyExistsException on duplicate")
        void shouldThrowFileAlreadyExistsExceptionOnDuplicate() {
            // given
            when(fileInfoRepository.save(any(FileInfo.class)))
                    .thenThrow(new DataIntegrityViolationException(MSG_DUPLICATE_KEY));

            // when & then
            assertThatThrownBy(() -> fileService.uploadFile(USER_ID, DIRECTORY, mockFile))
                    .isInstanceOf(FileAlreadyExistsException.class)
                    .hasMessageContaining(MSG_FILE_ALREADY_EXISTS);
        }

        @Test
        @DisplayName("Should rollback MinIO on duplicate")
        void shouldRollbackMinioOnDuplicate() {
            // given
            when(fileInfoRepository.save(any(FileInfo.class)))
                    .thenThrow(new DataIntegrityViolationException(MSG_DUPLICATE_KEY));

            ArgumentCaptor<String> storageKeyCaptor = ArgumentCaptor.forClass(String.class);

            // when
            try {
                fileService.uploadFile(USER_ID, DIRECTORY, mockFile);
            } catch (FileAlreadyExistsException ignored) {
            }

            // then
            verify(fileStorage).put(
                    any(InputStream.class),
                    storageKeyCaptor.capture(),
                    eq(SIZE),
                    eq(CONTENT_TYPE));
            verify(fileStorage).delete(storageKeyCaptor.getValue());
        }

        @Test
        @DisplayName("Should not call mapper on duplicate")
        void shouldNotCallMapperOnDuplicate() {
            // given
            when(fileInfoRepository.save(any(FileInfo.class)))
                    .thenThrow(new DataIntegrityViolationException(MSG_DUPLICATE_KEY));

            // when
            try {
                fileService.uploadFile(USER_ID, DIRECTORY, mockFile);
            } catch (FileAlreadyExistsException ignored) {
            }

            // then
            verify(fileInfoMapper, never()).toResourceDto(any(FileInfo.class));
        }
    }
}
