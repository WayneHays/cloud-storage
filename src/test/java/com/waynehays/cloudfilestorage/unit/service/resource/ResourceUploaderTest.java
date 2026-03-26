package com.waynehays.cloudfilestorage.unit.service.resource;

import com.waynehays.cloudfilestorage.component.converter.ResourceDtoConverterApi;
import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import com.waynehays.cloudfilestorage.dto.ObjectData;
import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.exception.ResourceStorageException;
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
    private ResourceMetadataServiceApi metadataService;

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
            ObjectData objectData = createObjectData(fullPath, 100L, "text/plain");
            ResourceDto fileDto = new ResourceDto("directory/", "file.txt", 100L, ResourceType.FILE);

            when(keyResolver.resolveKey(USER_ID, fullPath)).thenReturn(storageKey);
            when(converter.fileFromPath(fullPath, 100L)).thenReturn(fileDto);

            // when
            List<ResourceDto> result = resourceUploader.upload(USER_ID, List.of(objectData));

            // then
            assertThat(result).contains(fileDto);
            verify(metadataService).throwIfAnyExists(eq(USER_ID), any());
            verify(storage).putObject(any(InputStream.class), eq(storageKey), eq(100L), eq("text/plain"));
            verify(metadataService).saveFile(USER_ID, fullPath, 100L);
        }

        @Test
        void shouldUploadMultipleFiles() {
            // given
            String path1 = "directory/file1.txt";
            String path2 = "directory/file2.txt";
            String key1 = "user-1-files/directory/file1.txt";
            String key2 = "user-1-files/directory/file2.txt";
            ObjectData data1 = createObjectData(path1, 100L, "text/plain");
            ObjectData data2 = createObjectData(path2, 200L, "text/plain");
            ResourceDto dto1 = new ResourceDto("directory/", "file1.txt", 100L, ResourceType.FILE);
            ResourceDto dto2 = new ResourceDto("directory/", "file2.txt", 200L, ResourceType.FILE);

            when(keyResolver.resolveKey(USER_ID, path1)).thenReturn(key1);
            when(keyResolver.resolveKey(USER_ID, path2)).thenReturn(key2);
            when(converter.fileFromPath(path1, 100L)).thenReturn(dto1);
            when(converter.fileFromPath(path2, 200L)).thenReturn(dto2);

            // when
            List<ResourceDto> result = resourceUploader.upload(USER_ID, List.of(data1, data2));

            // then
            assertThat(result).contains(dto1, dto2);
            verify(storage).putObject(any(), eq(key1), eq(100L), any());
            verify(storage).putObject(any(), eq(key2), eq(200L), any());
            verify(metadataService).saveFile(USER_ID, path1, 100L);
            verify(metadataService).saveFile(USER_ID, path2, 200L);
        }

        @Test
        void shouldUseDefaultContentTypeWhenProvided() {
            // given
            String fullPath = "directory/file.txt";
            String storageKey = "user-1-files/directory/file.txt";
            ObjectData objectData = createObjectData(fullPath, 50L, "application/octet-stream");
            ResourceDto fileDto = new ResourceDto("directory/", "file.txt", 50L, ResourceType.FILE);

            when(keyResolver.resolveKey(USER_ID, fullPath)).thenReturn(storageKey);
            when(converter.fileFromPath(fullPath, 50L)).thenReturn(fileDto);

            // when
            resourceUploader.upload(USER_ID, List.of(objectData));

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
            ObjectData objectData = createObjectData(fullPath, 100L, "text/plain");

            doThrow(new ResourceAlreadyExistsException("Resource already exists", fullPath))
                    .when(metadataService).throwIfAnyExists(eq(USER_ID), any());

            // when & then
            assertThatThrownBy(() -> resourceUploader.upload(USER_ID, List.of(objectData)))
                    .isInstanceOf(ResourceAlreadyExistsException.class);

            verify(storage, never()).putObject(any(), any(), anyLong(), any());
        }

        @Test
        void shouldThrowWhenDuplicateFilesInRequest() {
            // given
            ObjectData data1 = createObjectData("directory/file.txt", 100L, "text/plain");
            ObjectData data2 = createObjectData("directory/file.txt", 200L, "text/plain");

            // when & then
            assertThatThrownBy(() -> resourceUploader.upload(USER_ID, List.of(data1, data2)))
                    .isInstanceOf(ResourceAlreadyExistsException.class);

            verify(storage, never()).putObject(any(), any(), anyLong(), any());
            verify(metadataService, never()).throwIfAnyExists(any(), any());
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
            ObjectData data1 = createObjectData(path1, 100L, "text/plain");
            ObjectData data2 = createObjectData(path2, 200L, "text/plain");

            when(keyResolver.resolveKey(USER_ID, path1)).thenReturn(key1);
            when(keyResolver.resolveKey(USER_ID, path2)).thenReturn(key2);
            when(converter.fileFromPath(path1, 100L)).thenReturn(
                    new ResourceDto("directory/", "file1.txt", 100L, ResourceType.FILE));
            doNothing().when(storage).putObject(any(), eq(key1), eq(100L), any());
            doThrow(new ResourceStorageException("Storage error"))
                    .when(storage).putObject(any(), eq(key2), eq(200L), any());

            // when & then
            List<ObjectData> files = List.of(data1, data2);
            assertThatThrownBy(() -> resourceUploader.upload(USER_ID, files))
                    .isInstanceOf(ResourceStorageException.class);

            verify(storage).deleteObject(key1);
            verify(storage, never()).deleteObject(key2);
            verify(metadataService).delete(USER_ID, path1);
            verify(metadataService, never()).delete(USER_ID, path2);
        }

        @Test
        void shouldRollbackStorageAndMetadataWhenDirectoryCreationFails() {
            // given
            String fullPath = "directory/subdirectory/file.txt";
            String storageKey = "user-1-files/directory/subdirectory/file.txt";
            String dirKey = "user-1-files/directory/";
            ObjectData objectData = createObjectData(fullPath, 100L, "text/plain");
            ResourceDto fileDto = new ResourceDto("directory/subdirectory/", "file.txt", 100L, ResourceType.FILE);

            when(keyResolver.resolveKey(USER_ID, fullPath)).thenReturn(storageKey);
            when(converter.fileFromPath(fullPath, 100L)).thenReturn(fileDto);
            when(metadataService.exists(USER_ID, "directory/")).thenReturn(false);
            when(keyResolver.resolveKey(USER_ID, "directory/")).thenReturn(dirKey);
            doThrow(new ResourceStorageException("Storage error"))
                    .when(storage).createDirectory(dirKey);

            // when & then
            assertThatThrownBy(() -> resourceUploader.upload(USER_ID, List.of(objectData)))
                    .isInstanceOf(ResourceStorageException.class);

            verify(storage).deleteObject(storageKey);
            verify(metadataService).delete(USER_ID, fullPath);
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
            ObjectData objectData = createObjectData(fullPath, 100L, "text/plain");
            ResourceDto fileDto = new ResourceDto("directory/subdirectory/", "file.txt", 100L, ResourceType.FILE);
            ResourceDto dirDto1 = new ResourceDto("", "directory/", null, ResourceType.DIRECTORY);
            ResourceDto dirDto2 = new ResourceDto("directory/", "subdirectory/", null, ResourceType.DIRECTORY);

            when(keyResolver.resolveKey(USER_ID, fullPath)).thenReturn(storageKey);
            when(keyResolver.resolveKey(USER_ID, "directory/")).thenReturn(dirKey1);
            when(keyResolver.resolveKey(USER_ID, "directory/subdirectory/")).thenReturn(dirKey2);
            when(converter.fileFromPath(fullPath, 100L)).thenReturn(fileDto);
            when(metadataService.exists(USER_ID, "directory/")).thenReturn(false);
            when(metadataService.exists(USER_ID, "directory/subdirectory/")).thenReturn(false);
            when(converter.directoryFromPath("directory/")).thenReturn(dirDto1);
            when(converter.directoryFromPath("directory/subdirectory/")).thenReturn(dirDto2);

            // when
            List<ResourceDto> result = resourceUploader.upload(USER_ID, List.of(objectData));

            // then
            assertThat(result).contains(dirDto1, dirDto2);
            verify(storage).createDirectory(dirKey1);
            verify(storage).createDirectory(dirKey2);
            verify(metadataService).saveDirectory(USER_ID, "directory/");
            verify(metadataService).saveDirectory(USER_ID, "directory/subdirectory/");
        }

        @Test
        void shouldNotCreateExistingDirectories() {
            // given
            String fullPath = "directory/file.txt";
            String storageKey = "user-1-files/directory/file.txt";
            ObjectData objectData = createObjectData(fullPath, 100L, "text/plain");
            ResourceDto fileDto = new ResourceDto("directory/", "file.txt", 100L, ResourceType.FILE);

            when(keyResolver.resolveKey(USER_ID, fullPath)).thenReturn(storageKey);
            when(converter.fileFromPath(fullPath, 100L)).thenReturn(fileDto);
            when(metadataService.exists(USER_ID, "directory/")).thenReturn(true);

            // when
            resourceUploader.upload(USER_ID, List.of(objectData));

            // then
            verify(storage, never()).createDirectory(any());
            verify(metadataService, never()).saveDirectory(any(), any());
        }

        @Test
        void shouldTrackCreatedDirectoriesInContext() {
            // given
            String fullPath = "directory/file.txt";
            String storageKey = "user-1-files/directory/file.txt";
            String dirKey = "user-1-files/directory/";
            ObjectData objectData = createObjectData(fullPath, 100L, "text/plain");
            ResourceDto fileDto = new ResourceDto("directory/", "file.txt", 100L, ResourceType.FILE);
            ResourceDto dirDto = new ResourceDto("", "directory/", null, ResourceType.DIRECTORY);

            when(keyResolver.resolveKey(USER_ID, fullPath)).thenReturn(storageKey);
            when(keyResolver.resolveKey(USER_ID, "directory/")).thenReturn(dirKey);
            when(converter.fileFromPath(fullPath, 100L)).thenReturn(fileDto);
            when(metadataService.exists(USER_ID, "directory/")).thenReturn(false);
            when(converter.directoryFromPath("directory/")).thenReturn(dirDto);

            // when
            List<ResourceDto> result = resourceUploader.upload(USER_ID, List.of(objectData));

            // then
            assertThat(result).contains(dirDto, fileDto);
        }
    }

    private ObjectData createObjectData(String fullPath, long size, String contentType) {
        return new ObjectData(
                null,
                null,
                null,
                fullPath,
                size,
                contentType,
                () -> new ByteArrayInputStream(new byte[0])
        );
    }
}
