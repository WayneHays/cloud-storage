package com.waynehays.cloudfilestorage.unit.component.validator;

import com.waynehays.cloudfilestorage.component.validator.UploadValidator;
import com.waynehays.cloudfilestorage.dto.ObjectData;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
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

@ExtendWith(MockitoExtension.class)
class UploadValidatorTest {

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private UploadValidator uploadValidator;

    private static final Long USER_ID = 1L;

    private ObjectData createObject(String fullPath) {
        return new ObjectData(
                "file.txt",
                "file.txt",
                "dir/",
                fullPath,
                100,
                "text/plain",
                InputStream::nullInputStream);
    }

    @Nested
    class DuplicatePaths {

        @Test
        void shouldPassWhenAllPathsUnique() {
            // given
            List<ObjectData> objects = List.of(
                    createObject("dir/a.txt"),
                    createObject("dir/b.txt")
            );

            // when & then
            assertThatNoException()
                    .isThrownBy(() -> uploadValidator.validate(USER_ID, objects));
        }

        @Test
        void shouldThrowWhenDuplicatePathsInRequest() {
            // given
            List<ObjectData> objects = List.of(
                    createObject("dir/same.txt"),
                    createObject("dir/same.txt")
            );

            // when & then
            assertThatThrownBy(() -> uploadValidator.validate(USER_ID, objects))
                    .isInstanceOf(ResourceAlreadyExistsException.class);
        }
    }
}
