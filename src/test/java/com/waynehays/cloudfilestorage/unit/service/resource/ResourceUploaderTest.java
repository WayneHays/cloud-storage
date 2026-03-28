package com.waynehays.cloudfilestorage.unit.service.resource;

import com.waynehays.cloudfilestorage.component.ResourceDtoConverter;
import com.waynehays.cloudfilestorage.component.validator.UploadValidator;
import com.waynehays.cloudfilestorage.dto.ObjectData;
import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.exception.ResourceStorageLimitException;
import com.waynehays.cloudfilestorage.exception.ResourceStorageOperationException;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.resource.uploader.ResourceUploader;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageKeyResolver;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceUploaderTest {

    @Mock
    private UploadValidator uploadValidator;

    @Mock
    private ResourceStorageApi resourceStorage;

    @Mock
    private ResourceStorageKeyResolver keyResolver;

    @Mock
    private ResourceDtoConverter dtoConverter;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private ResourceUploader resourceUploader;

    private static final Long USER_ID = 1L;

    private ObjectData createObject(String fullPath, long size) {
        return new ObjectData("file.txt", "file.txt", "dir/", fullPath, size,
                "text/plain", InputStream::nullInputStream);
    }

    @Nested
    class SuccessfulUpload {

        @Test
        void shouldUploadSingleFile() {
            // given
            ObjectData object = createObject("dir/file.txt", 100);
            List<ObjectData> objects = List.of(object);
            ResourceDto fileDto = new ResourceDto("dir/", "file.txt", 100L, ResourceType.FILE);

            when(keyResolver.resolveKey(USER_ID, "dir/file.txt")).thenReturn("1/dir/file.txt");
            when(dtoConverter.fileFromPath("dir/file.txt", 100L)).thenReturn(fileDto);
            when(metadataService.exists(USER_ID, "dir/")).thenReturn(false);
            when(keyResolver.resolveKey(USER_ID, "dir/")).thenReturn("1/dir/");
            when(dtoConverter.directoryFromPath("dir/")).thenReturn(
                    new ResourceDto("", "dir/", null, ResourceType.DIRECTORY));

            // when
            List<ResourceDto> result = resourceUploader.upload(USER_ID, objects);

            // then
            assertThat(result).hasSize(2);
            verify(uploadValidator).validate(USER_ID, objects);
            verify(resourceStorage).putObject(any(InputStream.class),
                    eq("1/dir/file.txt"), eq(100L), eq("text/plain"));
            verify(metadataService).saveFile(USER_ID, "dir/file.txt", 100L);
        }

        @Test
        void shouldUploadMultipleFiles() {
            // given
            ObjectData object1 = createObject("dir/a.txt", 100);
            ObjectData object2 = createObject("dir/b.txt", 200);
            List<ObjectData> objects = List.of(object1, object2);

            ResourceDto fileDto1 = new ResourceDto("dir/", "a.txt", 100L, ResourceType.FILE);
            ResourceDto fileDto2 = new ResourceDto("dir/", "b.txt", 200L, ResourceType.FILE);

            when(keyResolver.resolveKey(USER_ID, "dir/a.txt")).thenReturn("1/dir/a.txt");
            when(keyResolver.resolveKey(USER_ID, "dir/b.txt")).thenReturn("1/dir/b.txt");
            when(dtoConverter.fileFromPath("dir/a.txt", 100L)).thenReturn(fileDto1);
            when(dtoConverter.fileFromPath("dir/b.txt", 200L)).thenReturn(fileDto2);
            when(metadataService.exists(USER_ID, "dir/")).thenReturn(false);
            when(keyResolver.resolveKey(USER_ID, "dir/")).thenReturn("1/dir/");
            when(dtoConverter.directoryFromPath("dir/")).thenReturn(
                    new ResourceDto("", "dir/", null, ResourceType.DIRECTORY));

            // when
            List<ResourceDto> result = resourceUploader.upload(USER_ID, objects);

            // then
            assertThat(result).hasSize(3);
            verify(resourceStorage, times(2))
                    .putObject(any(), any(), anyLong(), any());
            verify(metadataService).saveFile(USER_ID, "dir/a.txt", 100L);
            verify(metadataService).saveFile(USER_ID, "dir/b.txt", 200L);
        }

        @Test
        void shouldSkipExistingDirectories() {
            // given
            ObjectData object = createObject("dir/file.txt", 100);
            List<ObjectData> objects = List.of(object);
            ResourceDto fileDto = new ResourceDto("dir/", "file.txt", 100L, ResourceType.FILE);

            when(keyResolver.resolveKey(USER_ID, "dir/file.txt")).thenReturn("1/dir/file.txt");
            when(dtoConverter.fileFromPath("dir/file.txt", 100L)).thenReturn(fileDto);
            when(metadataService.exists(USER_ID, "dir/")).thenReturn(true);

            // when
            List<ResourceDto> result = resourceUploader.upload(USER_ID, objects);

            // then
            assertThat(result).hasSize(1);
            verify(resourceStorage, never()).createDirectory(any());
            verify(metadataService, never()).saveDirectory(any(), any());
        }
    }

    @Nested
    class ValidationFailure {

        @Test
        void shouldPropagateValidationException() {
            // given
            List<ObjectData> objects = List.of(createObject("dir/file.txt", 100));
            doThrow(new ResourceStorageLimitException("No space", 100L, 0L))
                    .when(uploadValidator).validate(USER_ID, objects);

            // when & then
            assertThatThrownBy(() -> resourceUploader.upload(USER_ID, objects))
                    .isInstanceOf(ResourceStorageLimitException.class);

            verify(resourceStorage, never())
                    .putObject(any(), any(), anyLong(), any());
            verify(metadataService, never())
                    .saveFile(any(), any(), anyLong());
        }
    }

    @Nested
    class Rollback {

        @Test
        void shouldRollbackOnStorageFailure() {
            // given
            ObjectData object1 = createObject("dir/a.txt", 100);
            ObjectData object2 = createObject("dir/b.txt", 200);
            List<ObjectData> objects = List.of(object1, object2);

            when(keyResolver.resolveKey(USER_ID, "dir/a.txt")).thenReturn("1/dir/a.txt");
            when(keyResolver.resolveKey(USER_ID, "dir/b.txt")).thenReturn("1/dir/b.txt");
            when(dtoConverter.fileFromPath("dir/a.txt", 100L))
                    .thenReturn(new ResourceDto("dir/", "a.txt", 100L, ResourceType.FILE));

            doNothing().when(resourceStorage).putObject(any(), eq("1/dir/a.txt"), eq(100L), any());
            doThrow(new ResourceStorageOperationException("Storage error"))
                    .when(resourceStorage).putObject(any(), eq("1/dir/b.txt"), eq(200L), any());

            // when & then
            assertThatThrownBy(() -> resourceUploader.upload(USER_ID, objects))
                    .isInstanceOf(ResourceStorageOperationException.class);

            verify(resourceStorage).deleteObject("1/dir/a.txt");
            verify(metadataService).delete(USER_ID, "dir/a.txt");
        }

        @Test
        void shouldContinueRollbackWhenDeleteFails() {
            // given
            ObjectData object1 = createObject("dir/a.txt", 100);
            ObjectData object2 = createObject("dir/b.txt", 200);
            List<ObjectData> objects = List.of(object1, object2);

            when(keyResolver.resolveKey(USER_ID, "dir/a.txt")).thenReturn("1/dir/a.txt");
            when(keyResolver.resolveKey(USER_ID, "dir/b.txt")).thenReturn("1/dir/b.txt");
            when(dtoConverter.fileFromPath("dir/a.txt", 100L))
                    .thenReturn(new ResourceDto("dir/", "a.txt", 100L, ResourceType.FILE));

            doNothing().when(resourceStorage).putObject(any(), eq("1/dir/a.txt"), eq(100L), any());
            doThrow(new ResourceStorageOperationException("Storage error"))
                    .when(resourceStorage).putObject(any(), eq("1/dir/b.txt"), eq(200L), any());
            doThrow(new RuntimeException("Delete failed"))
                    .when(resourceStorage).deleteObject("1/dir/a.txt");

            // when & then
            assertThatThrownBy(() -> resourceUploader.upload(USER_ID, objects))
                    .isInstanceOf(ResourceStorageOperationException.class);

            verify(metadataService).delete(USER_ID, "dir/a.txt");
        }
    }
}
