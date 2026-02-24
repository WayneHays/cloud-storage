package com.waynehays.cloudfilestorage.unit.service.file.deleter;

import com.waynehays.cloudfilestorage.exception.FileNotFoundException;
import com.waynehays.cloudfilestorage.exception.FileStorageException;
import com.waynehays.cloudfilestorage.filestorage.MinioFileStorage;
import com.waynehays.cloudfilestorage.service.file.deleter.FileDeleterImpl;
import com.waynehays.cloudfilestorage.service.fileinfo.FileInfoServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileDeleterImplTest {
    private static final String STORAGE_KEY = "storageKey";
    private static final Long USER_ID = 1L;
    private static final String DIRECTORY = "directory";
    private static final String FILENAME = "file.txt";
    private static final String MSG_FILE_NOT_FOUND = "file not found";
    private static final String MSG_STORAGE_FAILED = "storage failed";

    @Mock
    private MinioFileStorage fileStorage;

    @Mock
    private FileInfoServiceImpl fileInfoService;

    @InjectMocks
    private FileDeleterImpl fileDeleter;

    @Test
    @DisplayName("Should call deps in correct order")
    void shouldCallDepsInCorrectOrder() {
        // given
        when(fileInfoService.deleteFileInfoAndReturnStorageKey(USER_ID, DIRECTORY, FILENAME))
                .thenReturn(STORAGE_KEY);
        doNothing()
                .when(fileStorage).delete(STORAGE_KEY);

        // when
        fileDeleter.delete(USER_ID, DIRECTORY, FILENAME);

        // then
        InOrder inOrder = Mockito.inOrder(fileInfoService, fileStorage);
        inOrder.verify(fileInfoService).deleteFileInfoAndReturnStorageKey(USER_ID, DIRECTORY, FILENAME);
        inOrder.verify(fileStorage).delete(STORAGE_KEY);
    }

    @Test
    @DisplayName("Should not call 'fileStorage.delete()' when 'fileInfoService' throw FileNotFoundException")
    void shouldNotCallMethodIfExceptionThrown() {
        // given
        when(fileInfoService.deleteFileInfoAndReturnStorageKey(USER_ID, DIRECTORY, FILENAME))
                .thenThrow(new FileNotFoundException(MSG_FILE_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> fileDeleter.delete(USER_ID, DIRECTORY, FILENAME))
                .isInstanceOf(FileNotFoundException.class)
                .hasMessageContaining(MSG_FILE_NOT_FOUND);
    }

    @Test
    @DisplayName("Should rethrow exception up when 'fileStorage.delete' throws FileStorageException")
    void shouldRethrowException() {
        // given
        when(fileInfoService.deleteFileInfoAndReturnStorageKey(USER_ID, DIRECTORY, FILENAME))
                .thenReturn(STORAGE_KEY);
        doThrow(new FileStorageException(MSG_STORAGE_FAILED))
                .when(fileStorage).delete(STORAGE_KEY);

        // when & then
        assertThatThrownBy(() -> fileDeleter.delete(USER_ID, DIRECTORY, FILENAME))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining(MSG_STORAGE_FAILED);
    }

}
