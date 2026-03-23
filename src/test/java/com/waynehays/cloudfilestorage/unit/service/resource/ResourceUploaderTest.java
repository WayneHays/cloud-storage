package com.waynehays.cloudfilestorage.unit.service.resource;

import com.waynehays.cloudfilestorage.component.converter.ResourceDtoConverterApi;
import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import com.waynehays.cloudfilestorage.dto.FileData;
import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.exception.FileStorageException;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
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

    @Mock
    private ResourceStorageApi storage;

    @Mock
    private StorageKeyResolverApi keyResolver;

    @Mock
    private ResourceDtoConverterApi converter;

    @Mock
    private ResourceMetadataServiceApi service;

    @InjectMocks
    private ResourceUploader resourceUploader;

    private static final Long USER_ID = 1L;

    @Nested
    class Upload {

        @Test
        void shouldUploadFileAndReturnResult() {
            // given
            String fullPath = "directory/file.txt";
            String storageKey = "user-1-files/directory/file.txt";
            FileData fileData = createFileData(fullPath, 100L, "text/plain");
            ResourceDto fileDto = new ResourceDto("directory/", "file.txt", 100L, ResourceType.FILE);

            when(service.exists(USER_ID, fullPath)).thenReturn(false);
            when(keyResolver.resolveKey(USER_ID, fullPath)).thenReturn(storageKey);
            when(converter.fileFromPath(fullPath, 100L)).thenReturn(fileDto);

            // when
            List<ResourceDto> result = resourceUploader.upload(USER_ID, List.of(fileData));

            // then
            assertThat(result).contains(fileDto);
            verify(storage).putObject(any(InputStream.class), eq(storageKey), eq(100L), eq("text/plain"));
            verify(service).saveFile(USER_ID, fullPath, 100L);
        }

        @Test
        void shouldUseDefaultContentTypeWhenNull() {
            // given
            String fullPath = "directory/file.bin";
            String storageKey = "user-1-files/directory/file.bin";
            FileData fileData = createFileData(fullPath, 50L, null);
            ResourceDto fileDto = new ResourceDto("directory/", "file.bin", 50L, ResourceType.FILE);

            when(service.exists(USER_ID, fullPath)).thenReturn(false);
            when(keyResolver.resolveKey(USER_ID, fullPath)).thenReturn(storageKey);
            when(converter.fileFromPath(fullPath, 50L)).thenReturn(fileDto);

            // when
            resourceUploader.upload(USER_ID, List.of(fileData));

            // then
            verify(storage).putObject(any(InputStream.class), eq(storageKey), eq(50L), eq("application/octet-stream"));
        }
    }

    @Nested
    class CheckForDuplicates {

        @Test
        void shouldThrowWhenDuplicateExistsInStorage() {
            // given
            String fullPath = "directory/file.txt";
            FileData fileData = createFileData(fullPath, 100L, "text/plain");
            List<FileData> files = List.of(fileData);

            when(service.exists(USER_ID, fullPath)).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> resourceUploader.upload(USER_ID, files))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining(fullPath);

            verify(storage, never()).putObject(any(), any(), anyLong(), any());
        }

        @Test
        void shouldThrowWhenDuplicateFilesInRequest() {
            // given
            FileData fileData1 = createFileData("directory/file.txt", 100L, "text/plain");
            FileData fileData2 = createFileData("directory/file.txt", 200L, "text/plain");
            List<FileData> files = List.of(fileData1, fileData2);

            // when & then
            assertThatThrownBy(() -> resourceUploader.upload(USER_ID, files))
                    .isInstanceOf(ResourceAlreadyExistsException.class);

            verify(storage, never()).putObject(any(), any(), anyLong(), any());
        }
    }

    @Nested
    class Rollback {

        @Test
        void shouldRollbackStorageAndMetadataOnFailure() {
            // given
            String path1 = "directory/file1.txt";
            String path2 = "directory/file2.txt";
            String key1 = "user-1-files/directory/file1.txt";
            String key2 = "user-1-files/directory/file2.txt";
            FileData fileData1 = createFileData(path1, 100L, "text/plain");
            FileData fileData2 = createFileData(path2, 200L, "text/plain");

            when(service.exists(eq(USER_ID), any())).thenReturn(false);
            when(keyResolver.resolveKey(USER_ID, path1)).thenReturn(key1);
            when(keyResolver.resolveKey(USER_ID, path2)).thenReturn(key2);
            when(converter.fileFromPath(path1, 100L)).thenReturn(
                    new ResourceDto("directory/", "file1.txt", 100L, ResourceType.FILE));
            doNothing().when(storage).putObject(any(), eq(key1), eq(100L), any());
            doThrow(new FileStorageException("Storage error"))
                    .when(storage).putObject(any(), eq(key2), eq(200L), any());

            // when & then
            List<FileData> files = List.of(fileData1, fileData2);
            assertThatThrownBy(() -> resourceUploader.upload(USER_ID, files))
                    .isInstanceOf(FileStorageException.class);

            verify(storage).delete(key1);
            verify(storage, never()).delete(key2);
            verify(service).delete(USER_ID, path1);
            verify(service, never()).delete(USER_ID, path2);
        }
    }

    @Nested
    class CreateDirectories {

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

            when(service.exists(USER_ID, fullPath)).thenReturn(false);
            when(keyResolver.resolveKey(USER_ID, fullPath)).thenReturn(storageKey);
            when(keyResolver.resolveKey(USER_ID, "directory/")).thenReturn(dirKey1);
            when(keyResolver.resolveKey(USER_ID, "directory/subdirectory/")).thenReturn(dirKey2);
            when(converter.fileFromPath(fullPath, 100L)).thenReturn(fileDto);
            when(service.exists(USER_ID, "directory/")).thenReturn(false);
            when(service.exists(USER_ID, "directory/subdirectory/")).thenReturn(false);
            when(converter.directoryFromPath("directory/")).thenReturn(dirDto1);
            when(converter.directoryFromPath("directory/subdirectory/")).thenReturn(dirDto2);

            // when
            List<ResourceDto> result = resourceUploader.upload(USER_ID, List.of(fileData));

            // then
            assertThat(result).contains(dirDto1, dirDto2);
            verify(storage).createDirectory(dirKey1);
            verify(storage).createDirectory(dirKey2);
            verify(service).saveDirectory(USER_ID, "directory/");
            verify(service).saveDirectory(USER_ID, "directory/subdirectory/");
        }

        @Test
        void shouldNotCreateExistingDirectories() {
            // given
            String fullPath = "directory/file.txt";
            String storageKey = "user-1-files/directory/file.txt";
            FileData fileData = createFileData(fullPath, 100L, "text/plain");
            ResourceDto fileDto = new ResourceDto("directory/", "file.txt", 100L, ResourceType.FILE);

            when(service.exists(USER_ID, fullPath)).thenReturn(false);
            when(keyResolver.resolveKey(USER_ID, fullPath)).thenReturn(storageKey);
            when(converter.fileFromPath(fullPath, 100L)).thenReturn(fileDto);
            when(service.exists(USER_ID, "directory/")).thenReturn(true);

            // when
            resourceUploader.upload(USER_ID, List.of(fileData));

            // then
            verify(storage, never()).createDirectory(any());
            verify(service, never()).saveDirectory(any(), any());
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
