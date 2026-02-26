package com.waynehays.cloudfilestorage.unit.service.fileinfo;

import com.waynehays.cloudfilestorage.dto.files.FileData;
import com.waynehays.cloudfilestorage.entity.FileInfo;
import com.waynehays.cloudfilestorage.entity.User;
import com.waynehays.cloudfilestorage.exception.FileAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.FileNotFoundException;
import com.waynehays.cloudfilestorage.repository.FileInfoRepository;
import com.waynehays.cloudfilestorage.repository.UserRepository;
import com.waynehays.cloudfilestorage.service.fileinfo.FileInfoServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.io.ByteArrayInputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileInfoServiceImplTest {
    private static final Long USER_ID = 1L;
    private static final Long FILE_INFO_ID = 10L;
    private static final long SIZE = 1024L;
    private static final String DIRECTORY = "directory";
    private static final String FILENAME = "file.txt";
    private static final String STORAGE_KEY = "key";
    private static final String CONTENT_TYPE = "content-type";
    private static final String EXTENSION = "txt";
    private static final String ORIGINAL_FILENAME = "originalFileName";
    private static final String NEW_DIRECTORY = "newDirectory";
    private static final String NEW_FILENAME = "newFileName";
    private static final String NEW_STORAGE_KEY = "newStorageKey";

    @Mock
    private FileInfoRepository fileInfoRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FileInfoServiceImpl fileInfoService;

    private final User user = new User(1L, "name", "password");

    @Test
    @DisplayName("Should return FileInfo with correct fields when save successful")
    void shouldReturnCorrectFileInfo_whenSaveSuccessful() {
        // given
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        when(fileInfoRepository.save(any(FileInfo.class))).thenAnswer(invocation -> {
            FileInfo created = invocation.getArgument(0);
            created.setId(FILE_INFO_ID);
            return created;
        });

        // when
        FileInfo result = fileInfoService.save(USER_ID, createFileData(), STORAGE_KEY);

        // then
        assertThat(result.getId()).isEqualTo(FILE_INFO_ID);
        assertThat(result.getDirectory()).isEqualTo(DIRECTORY);
        assertThat(result.getName()).isEqualTo(FILENAME);
        assertThat(result.getStorageKey()).isEqualTo(STORAGE_KEY);
        assertThat(result.getSize()).isEqualTo(SIZE);
        assertThat(result.getContentType()).isEqualTo(CONTENT_TYPE);
        assertThat(result.getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("Should throw FileAlreadyExistsException when duplicate name")
    void shouldThrowException_whenDuplicateFilename() {
        // given
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        when(fileInfoRepository.save(any(FileInfo.class)))
                .thenThrow(new DataIntegrityViolationException("message"));
        FileData fileData = createFileData();

        // when & then
        assertThatThrownBy(() -> fileInfoService.save(USER_ID, fileData, STORAGE_KEY))
                .isInstanceOf(FileAlreadyExistsException.class);
    }

    @Test
    @DisplayName("Should return FileInfo when found")
    void shouldReturnFileInfo_whenFound() {
        // given
        FileInfo fileInfo = createPersistedFileInfo();
        when(fileInfoRepository.findByUserIdAndDirectoryAndName(USER_ID, DIRECTORY, FILENAME))
                .thenReturn(Optional.of(fileInfo));

        // when
        FileInfo result = fileInfoService.find(USER_ID, DIRECTORY, FILENAME);

        // then
        assertThat(result).isEqualTo(fileInfo);
    }

    @Test
    @DisplayName("Should throw FileNotFoundException when file not found")
    void shouldThrowException_whenFileInfoNotFound() {
        // given
        when(fileInfoRepository.findByUserIdAndDirectoryAndName(USER_ID, DIRECTORY, FILENAME))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> fileInfoService.find(USER_ID, DIRECTORY, FILENAME))
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    @DisplayName("Should call repository delete")
    void shouldCallRepositoryDelete() {
        // when
        fileInfoService.delete(USER_ID, DIRECTORY, FILENAME);

        // then
        verify(fileInfoRepository).deleteByUserIdAndDirectoryAndName(USER_ID, DIRECTORY, FILENAME);
    }

    @Test
    @DisplayName("Should delete and return storage key when file exists")
    void shouldDeleteAndReturnStorageKey() {
        // given
        when(fileInfoRepository.deleteAndReturnStorageKey(USER_ID, DIRECTORY, FILENAME))
                .thenReturn(Optional.of(STORAGE_KEY));

        // when
        String result = fileInfoService.deleteAndReturnStorageKey(USER_ID, DIRECTORY, FILENAME);

        // then
        assertThat(result).isEqualTo(STORAGE_KEY);
    }

    @Test
    @DisplayName("Should throw FileNotFoundException and not delete when file not found")
    void shouldThrowAndNotDelete_whenFileNotFoundForDelete() {
        // when
        assertThatThrownBy(() -> fileInfoService.deleteAndReturnStorageKey(USER_ID, DIRECTORY, FILENAME))
                .isInstanceOf(FileNotFoundException.class);

        // then
        verify(fileInfoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should update directory, name and storageKey when move successful")
    void shouldMove() {
        // given
        FileInfo fileInfo = createPersistedFileInfo();
        when(fileInfoRepository.findByUserIdAndDirectoryAndName(USER_ID, DIRECTORY, FILENAME))
                .thenReturn(Optional.of(fileInfo));
        when(fileInfoRepository.save(fileInfo)).thenReturn(fileInfo);

        // when
        FileInfo result = fileInfoService.move(
                USER_ID, DIRECTORY, FILENAME, NEW_DIRECTORY, NEW_FILENAME, NEW_STORAGE_KEY);

        // then
        assertThat(result.getDirectory()).isEqualTo(NEW_DIRECTORY);
        assertThat(result.getName()).isEqualTo(NEW_FILENAME);
        assertThat(result.getStorageKey()).isEqualTo(NEW_STORAGE_KEY);
    }

    @Test
    @DisplayName("Should throw FileAlreadyExistsException when target already exists")
    void shouldThrowException_whenMoveTargetExists() {
        // given
        FileInfo fileInfo = createPersistedFileInfo();
        when(fileInfoRepository.findByUserIdAndDirectoryAndName(USER_ID, DIRECTORY, FILENAME))
                .thenReturn(Optional.of(fileInfo));
        when(fileInfoRepository.save(fileInfo))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        // when & then
        assertThatThrownBy(() -> fileInfoService.move(
                USER_ID, DIRECTORY, FILENAME, NEW_DIRECTORY, NEW_FILENAME, NEW_STORAGE_KEY))
                .isInstanceOf(FileAlreadyExistsException.class);
    }

    @Test
    @DisplayName("Should throw FileNotFoundException when source file not found for move")
    void shouldThrowException_whenSourceNotFoundForMove() {
        // given
        when(fileInfoRepository.findByUserIdAndDirectoryAndName(USER_ID, DIRECTORY, FILENAME))
                .thenReturn(Optional.empty());

        // when
        assertThatThrownBy(() -> fileInfoService.move(
                USER_ID, DIRECTORY, FILENAME, NEW_DIRECTORY, NEW_FILENAME, NEW_STORAGE_KEY))
                .isInstanceOf(FileNotFoundException.class);

        // then
        verify(fileInfoRepository, never()).save(any());
    }

    private FileInfo createPersistedFileInfo() {
        return FileInfo.builder()
                .id(FILE_INFO_ID)
                .directory(DIRECTORY)
                .name(FILENAME)
                .storageKey(STORAGE_KEY)
                .size(SIZE)
                .contentType(CONTENT_TYPE)
                .user(user)
                .build();
    }

    private FileData createFileData() {
        return FileData.builder()
                .originalFilename(ORIGINAL_FILENAME)
                .filename(FILENAME)
                .directory(DIRECTORY)
                .extension(EXTENSION)
                .size(SIZE)
                .contentType(CONTENT_TYPE)
                .inputStream(new ByteArrayInputStream(new byte[0]))
                .build();
    }
}
