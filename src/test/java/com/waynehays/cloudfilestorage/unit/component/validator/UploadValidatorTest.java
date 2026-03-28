package com.waynehays.cloudfilestorage.unit.component.validator;

import com.waynehays.cloudfilestorage.component.validator.UploadValidator;
import com.waynehays.cloudfilestorage.dto.ObjectData;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.ResourceStorageLimitException;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.user.UserServiceApi;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadValidatorTest {

    @Mock
    private UserServiceApi userService;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private UploadValidator uploadValidator;

    private static final Long USER_ID = 1L;

    private ObjectData createObject(String fullPath, long size) {
        return new ObjectData(
                "file.txt",
                "file.txt",
                "dir/",
                fullPath,
                size,
                "text/plain",
                InputStream::nullInputStream);
    }

    @Nested
    class StorageLimit {

        @Test
        void shouldPassWhenUploadFitsInFreeSpace() {
            // given
            List<ObjectData> objects = List.of(createObject("dir/file.txt", 100));
            when(userService.getUserStorageLimit(USER_ID)).thenReturn(1000L);
            when(metadataService.getUsedSpace(USER_ID)).thenReturn(500L);

            // when & then
            assertThatNoException()
                    .isThrownBy(() -> uploadValidator.validate(USER_ID, objects));
        }

        @Test
        void shouldPassWhenUploadExactlyFillsFreeSpace() {
            // given
            List<ObjectData> objects = List.of(createObject("dir/file.txt", 500));
            when(userService.getUserStorageLimit(USER_ID)).thenReturn(1000L);
            when(metadataService.getUsedSpace(USER_ID)).thenReturn(500L);

            // when & then
            assertThatNoException()
                    .isThrownBy(() -> uploadValidator.validate(USER_ID, objects));
        }

        @Test
        void shouldThrowWhenUploadExceedsFreeSpace() {
            // given
            List<ObjectData> objects = List.of(createObject("dir/file.txt", 501));
            when(userService.getUserStorageLimit(USER_ID)).thenReturn(1000L);
            when(metadataService.getUsedSpace(USER_ID)).thenReturn(500L);

            // when & then
            assertThatThrownBy(() -> uploadValidator.validate(USER_ID, objects))
                    .isInstanceOf(ResourceStorageLimitException.class);
        }

        @Test
        void shouldSumSizesOfMultipleFiles() {
            // given
            List<ObjectData> objects = List.of(
                    createObject("dir/a.txt", 300),
                    createObject("dir/b.txt", 300)
            );
            when(userService.getUserStorageLimit(USER_ID)).thenReturn(1000L);
            when(metadataService.getUsedSpace(USER_ID)).thenReturn(500L);

            // when & then
            assertThatThrownBy(() -> uploadValidator.validate(USER_ID, objects))
                    .isInstanceOf(ResourceStorageLimitException.class);
        }

        @Test
        void shouldThrowWhenNoFreeSpaceLeft() {
            // given
            List<ObjectData> objects = List.of(createObject("dir/file.txt", 1));
            when(userService.getUserStorageLimit(USER_ID)).thenReturn(1000L);
            when(metadataService.getUsedSpace(USER_ID)).thenReturn(1000L);

            // when & then
            assertThatThrownBy(() -> uploadValidator.validate(USER_ID, objects))
                    .isInstanceOf(ResourceStorageLimitException.class);
        }
    }

    @Nested
    class DuplicatePaths {

        @Test
        void shouldPassWhenAllPathsUnique() {
            // given
            List<ObjectData> objects = List.of(
                    createObject("dir/a.txt", 100),
                    createObject("dir/b.txt", 100)
            );
            when(userService.getUserStorageLimit(USER_ID)).thenReturn(1000L);
            when(metadataService.getUsedSpace(USER_ID)).thenReturn(0L);

            // when & then
            assertThatNoException()
                    .isThrownBy(() -> uploadValidator.validate(USER_ID, objects));
        }

        @Test
        void shouldThrowWhenDuplicatePathsInRequest() {
            // given
            List<ObjectData> objects = List.of(
                    createObject("dir/same.txt", 100),
                    createObject("dir/same.txt", 100)
            );
            when(userService.getUserStorageLimit(USER_ID)).thenReturn(1000L);
            when(metadataService.getUsedSpace(USER_ID)).thenReturn(0L);

            // when & then
            assertThatThrownBy(() -> uploadValidator.validate(USER_ID, objects))
                    .isInstanceOf(ResourceAlreadyExistsException.class);
        }

        @Test
        void shouldThrowWhenPathAlreadyExistsInStorage() {
            // given
            List<ObjectData> objects = List.of(createObject("dir/existing.txt", 100));
            when(userService.getUserStorageLimit(USER_ID)).thenReturn(1000L);
            when(metadataService.getUsedSpace(USER_ID)).thenReturn(0L);
            doThrow(new ResourceAlreadyExistsException("Already exists", List.of("dir/existing.txt")))
                    .when(metadataService).throwIfAnyExists(USER_ID, List.of("dir/existing.txt"));

            // when & then
            assertThatThrownBy(() -> uploadValidator.validate(USER_ID, objects))
                    .isInstanceOf(ResourceAlreadyExistsException.class);
        }
    }
}
