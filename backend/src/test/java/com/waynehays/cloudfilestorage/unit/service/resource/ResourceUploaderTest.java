package com.waynehays.cloudfilestorage.unit.service.resource;

import com.waynehays.cloudfilestorage.component.ResourceDtoConverter;
import com.waynehays.cloudfilestorage.component.validator.UploadValidator;
import com.waynehays.cloudfilestorage.dto.ObjectData;
import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.ResourceStorageLimitException;
import com.waynehays.cloudfilestorage.exception.ResourceStorageOperationException;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.storagequota.StorageQuotaServiceApi;
import com.waynehays.cloudfilestorage.service.resource.uploader.ResourceUploader;
import com.waynehays.cloudfilestorage.storage.ResourceStorageApi;
import com.waynehays.cloudfilestorage.storage.ResourceStorageKeyResolverApi;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceUploaderTest {

    @Mock
    private UploadValidator uploadValidator;

    @Mock
    private ResourceDtoConverter dtoConverter;

    @Mock
    private ResourceStorageApi resourceStorage;

    @Mock
    private StorageQuotaServiceApi quotaService;

    @Mock
    private ResourceStorageKeyResolverApi keyResolver;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private ResourceUploader resourceUploader;

    private static final Long USER_ID = 1L;

    private ObjectData createObject(String fullPath, long size) {
        return new ObjectData(
                "file.txt",
                "file.txt",
                "dir/",
                fullPath,
                size,
                "text/plain",
                InputStream::nullInputStream
        );
    }

    @Nested
    class UploadSuccess {

        @Test
        void shouldReserveSpaceAndUploadFile() {
            // given
            ObjectData object = createObject("dir/file.txt", 500);
            String storageKey = "user-1-files/dir/file.txt";
            ResourceDto fileDto = new ResourceDto("dir/", "file.txt", 500L, ResourceType.FILE);

            when(keyResolver.resolveKey(USER_ID, "dir/file.txt")).thenReturn(storageKey);
            when(dtoConverter.fileFromPath("dir/file.txt", 500L)).thenReturn(fileDto);
            when(metadataService.exists(eq(USER_ID), anyString())).thenReturn(true);

            // when
            List<ResourceDto> result = resourceUploader.upload(USER_ID, List.of(object));

            // then
            InOrder inOrder = inOrder(uploadValidator, quotaService, resourceStorage, metadataService);
            inOrder.verify(uploadValidator).validate(USER_ID, List.of(object));
            inOrder.verify(quotaService).reserveSpace(USER_ID, 500L);
            inOrder.verify(resourceStorage).putObject(any(), eq(storageKey), eq(500L), eq("text/plain"));
            inOrder.verify(metadataService).saveFile(USER_ID, "dir/file.txt", 500L);

            assertThat(result).contains(fileDto);
        }

        @Test
        void shouldSumTotalSizeForMultipleFiles() {
            // given
            ObjectData file1 = createObject("dir/a.txt", 300);
            ObjectData file2 = createObject("dir/b.txt", 200);

            when(keyResolver.resolveKey(eq(USER_ID), anyString())).thenReturn("key");
            when(dtoConverter.fileFromPath(anyString(), anyLong()))
                    .thenReturn(new ResourceDto("dir/", "a.txt", 300L, ResourceType.FILE));
            when(metadataService.exists(eq(USER_ID), anyString())).thenReturn(true);

            // when
            resourceUploader.upload(USER_ID, List.of(file1, file2));

            // then
            verify(quotaService).reserveSpace(USER_ID, 500L);
        }
    }

    @Nested
    class UploadFailure {

        @Test
        void shouldReleaseSpaceOnStorageFailure() {
            // given
            ObjectData object = createObject("dir/file.txt", 500);
            String storageKey = "user-1-files/dir/file.txt";

            when(keyResolver.resolveKey(USER_ID, "dir/file.txt")).thenReturn(storageKey);
            doThrow(new ResourceStorageOperationException("MinIO down"))
                    .when(resourceStorage).putObject(any(), eq(storageKey), eq(500L), eq("text/plain"));

            // when & then
            List<ObjectData> objects = List.of(object);
            assertThatThrownBy(() -> resourceUploader.upload(USER_ID, objects))
                    .isInstanceOf(ResourceStorageOperationException.class);

            verify(quotaService).reserveSpace(USER_ID, 500);
            verify(quotaService).releaseSpace(USER_ID, 500);
        }

        @Test
        void shouldNotReserveSpaceOnValidationFailure() {
            // given
            ObjectData object = createObject("dir/file.txt", 500);

            List<ObjectData> objects = List.of(object);
            doThrow(new ResourceAlreadyExistsException("Duplicate", "dir/file.txt"))
                    .when(uploadValidator).validate(USER_ID, objects);

            // when & then
            assertThatThrownBy(() -> resourceUploader.upload(USER_ID, objects))
                    .isInstanceOf(ResourceAlreadyExistsException.class);

            verify(quotaService, never()).reserveSpace(any(), anyLong());
        }

        @Test
        void shouldNotReserveSpaceOnQuotaExceeded() {
            // given
            ObjectData object = createObject("dir/file.txt", 500);

            doThrow(new ResourceStorageLimitException("Not enough space", 500L, 100L))
                    .when(quotaService).reserveSpace(USER_ID, 500L);

            // when & then
            List<ObjectData> objects = List.of(object);
            assertThatThrownBy(() -> resourceUploader.upload(USER_ID, objects))
                    .isInstanceOf(ResourceStorageLimitException.class);

            verify(resourceStorage, never()).putObject(any(), any(), anyLong(), any());
        }
    }
}
