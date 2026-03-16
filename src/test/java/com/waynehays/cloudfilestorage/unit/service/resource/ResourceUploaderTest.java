package com.waynehays.cloudfilestorage.unit.service.resource;

import com.waynehays.cloudfilestorage.component.converter.ResourceDtoConverterApi;
import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import com.waynehays.cloudfilestorage.dto.FileData;
import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.exception.FileStorageException;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.filestorage.minio.MinioFileStorage;
import com.waynehays.cloudfilestorage.service.resource.uploader.ResourceUploader;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceUploaderTest {
    private static final Long USER_ID = 1L;

    @Mock
    private MinioFileStorage fileStorage;

    @Mock
    private StorageKeyResolverApi keyResolver;

    @Mock
    private ResourceDtoConverterApi dtoConverter;

    @InjectMocks
    private ResourceUploader resourceUploader;

    @Nested
    class Upload {

        @Test
        void shouldUploadFilesAndReturnResult() {
            // given
            String fullPath = "directory/file.txt";
            String storageKey = "user-1-files/directory/file.txt";
            FileData fileData = createFileData(fullPath, 100L, "text/plain");
            ResourceDto fileDto = new ResourceDto("directory/", "file.txt", 100L, ResourceType.FILE);

            when(keyResolver.resolveKey(USER_ID, fullPath)).thenReturn(storageKey);
            when(fileStorage.exists(storageKey)).thenReturn(false);
            when(dtoConverter.fileFromPath(fullPath, 100L)).thenReturn(fileDto);

            // when
            List<ResourceDto> result = resourceUploader.upload(USER_ID, List.of(fileData));

            // then
            assertThat(result).contains(fileDto);
            verify(fileStorage).putObject(any(InputStream.class), eq(storageKey), eq(100L), eq("text/plain"));
        }

        @Test
        void shouldUseDefaultContentTypeWhenNull() {
            // given
            String fullPath = "directory/file.bin";
            String storageKey = "user-1-files/directory/file.bin";
            FileData fileData = createFileData(fullPath, 50L, null);

            when(keyResolver.resolveKey(USER_ID, fullPath)).thenReturn(storageKey);
            when(fileStorage.exists(storageKey)).thenReturn(false);
            when(dtoConverter.fileFromPath(fullPath, 50L)).thenReturn(
                    new ResourceDto("directory/", "file.bin", 50L, ResourceType.FILE));

            // when
            resourceUploader.upload(USER_ID, List.of(fileData));

            // then
            verify(fileStorage).putObject(any(InputStream.class), eq(storageKey), eq(50L), eq("application/octet-stream"));
        }
    }

    @Nested
    class CheckForDuplicates {

        @Test
        void shouldThrowWhenDuplicateExists() {
            // given
            String fullPath = "directory/file.txt";
            String storageKey = "user-1-files/directory/file.txt";
            FileData fileData = createFileData(fullPath, 100L, "text/plain");

            when(keyResolver.resolveKey(USER_ID, fullPath)).thenReturn(storageKey);
            when(fileStorage.exists(storageKey)).thenReturn(true);

            // when & then
            List<FileData> files = List.of(fileData);
            assertThatThrownBy(() -> resourceUploader.upload(USER_ID, files))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining(fullPath);

            verify(fileStorage, never()).putObject(any(), any(), anyLong(), any());
        }
    }

    @Nested
    class Rollback {

        @Test
        void shouldRollbackUploadedFilesOnFailure() {
            // given
            String path1 = "directory/file1.txt";
            String path2 = "directory/file2.txt";
            String key1 = "user-1-files/directory/file1.txt";
            String key2 = "user-1-files/directory/file2.txt";
            FileData fileData1 = createFileData(path1, 100L, "text/plain");
            FileData fileData2 = createFileData(path2, 200L, "text/plain");

            when(keyResolver.resolveKey(USER_ID, path1)).thenReturn(key1);
            when(keyResolver.resolveKey(USER_ID, path2)).thenReturn(key2);
            when(fileStorage.exists(any())).thenReturn(false);
            when(dtoConverter.fileFromPath(path1, 100L)).thenReturn(
                    new ResourceDto("directory/", "file1.txt", 100L, ResourceType.FILE));
            doNothing().when(fileStorage).putObject(any(), eq(key1), eq(100L), any());
            doThrow(new FileStorageException("Storage error"))
                    .when(fileStorage).putObject(any(), eq(key2), eq(200L), any());

            // when & then
            List<FileData> files = List.of(fileData1, fileData2);
            assertThatThrownBy(() -> resourceUploader.upload(USER_ID, files))
                    .isInstanceOf(FileStorageException.class);

            verify(fileStorage).delete(key1);
            verify(fileStorage, never()).delete(key2);
        }
    }

    @Nested
    class CollectDirectories {

        @Test
        void shouldCreateNewDirectories() {
            // given
            String fullPath = "directory/subdirectory/file.txt";
            String storageKey = "user-1-files/directory/subdirectory/file.txt";
            String dirKey1 = "user-1-files/directory/";
            String dirKey2 = "user-1-files/directory/subdirectory/";
            FileData fileData = createFileData(fullPath, 100L, "text/plain");
            ResourceDto fileDto = new ResourceDto("directory/subdirectory/", "file.txt", 100L, ResourceType.FILE);
            ResourceDto dirDto1 = new ResourceDto("", "directory", null, ResourceType.DIRECTORY);
            ResourceDto dirDto2 = new ResourceDto("directory/", "subdirectory", null, ResourceType.DIRECTORY);

            when(keyResolver.resolveKey(USER_ID, fullPath)).thenReturn(storageKey);
            when(fileStorage.exists(storageKey)).thenReturn(false);
            when(dtoConverter.fileFromPath(fullPath, 100L)).thenReturn(fileDto);
            when(keyResolver.resolveKeyToDirectory(USER_ID, "directory")).thenReturn(dirKey1);
            when(keyResolver.resolveKeyToDirectory(USER_ID, "directory/subdirectory")).thenReturn(dirKey2);
            when(fileStorage.exists(dirKey1)).thenReturn(false);
            when(fileStorage.exists(dirKey2)).thenReturn(false);
            when(dtoConverter.directoryFromPath("directory")).thenReturn(dirDto1);
            when(dtoConverter.directoryFromPath("directory/subdirectory")).thenReturn(dirDto2);

            // when
            List<ResourceDto> result = resourceUploader.upload(USER_ID, List.of(fileData));

            // then
            assertThat(result).contains(dirDto1, dirDto2);
            verify(fileStorage).createDirectory(dirKey1);
            verify(fileStorage).createDirectory(dirKey2);
        }

        @Test
        void shouldNotCreateExistingDirectories() {
            // given
            String fullPath = "directory/file.txt";
            String storageKey = "user-1-files/directory/file.txt";
            String dirKey = "user-1-files/directory/";
            FileData fileData = createFileData(fullPath, 100L, "text/plain");
            ResourceDto fileDto = new ResourceDto("directory/", "file.txt", 100L, ResourceType.FILE);

            when(keyResolver.resolveKey(USER_ID, fullPath)).thenReturn(storageKey);
            when(fileStorage.exists(storageKey)).thenReturn(false);
            when(dtoConverter.fileFromPath(fullPath, 100L)).thenReturn(fileDto);
            when(keyResolver.resolveKeyToDirectory(USER_ID, "directory")).thenReturn(dirKey);
            when(fileStorage.exists(dirKey)).thenReturn(true);

            // when
            resourceUploader.upload(USER_ID, List.of(fileData));

            // then
            verify(fileStorage, never()).createDirectory(any());
        }
    }

    private FileData createFileData(String fullPath, long size, String contentType) {
        return FileData.builder()
                .fullPath(fullPath)
                .size(size)
                .contentType(contentType)
                .inputStreamSupplier(() -> new ByteArrayInputStream(new byte[0]))
                .build();
    }
}
