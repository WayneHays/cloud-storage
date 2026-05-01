package com.waynehays.cloudfilestorage.files.operation.upload;

import com.waynehays.cloudfilestorage.core.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceAlreadyExistsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class ValidateStepTest extends BaseUploadStepTest {

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private ValidateStep validateStep;

    @Test
    @DisplayName("Should pass when no duplicates and no type conflicts")
    void shouldPassWhenNoDuplicatesAndNoConflicts() {
        // given
        UploadContext context = uploadContext(
                uploadObject("key-1", "user/1/file1.txt", 100),
                uploadObject("key-2", "user/1/file2.txt", 200)
        );

        // when & then
        assertThatNoException().isThrownBy(() -> validateStep.execute(context));
    }

    @Test
    @DisplayName("Should throw when paths already exist")
    void shouldThrowWhenPathsAlreadyExist() {
        // given
        UploadContext context = uploadContext(
                uploadObject("key-1", "user/1/file1.txt", 100)
        );
        doThrow(new ResourceAlreadyExistsException("Resources already exist", List.of("user/1/file1.txt")))
                .when(metadataService).throwIfAnyExists(any(), any());

        // when & then
        assertThatThrownBy(() -> validateStep.execute(context))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    @DisplayName("Should throw when directory with same name exists")
    void shouldThrowWhenConflictingTypeExists() {
        // given
        UploadContext context = uploadContext(
                uploadObject("key-1", "user/1/report", 100)
        );
        doThrow(new ResourceAlreadyExistsException(
                "Resources with same name, but different type already exist", List.of("report/")))
                .when(metadataService).throwIfAnyConflictingTypeExists(any(), any());

        // when & then
        assertThatThrownBy(() -> validateStep.execute(context))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }
}
