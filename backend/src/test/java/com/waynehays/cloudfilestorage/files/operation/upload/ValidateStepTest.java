package com.waynehays.cloudfilestorage.files.operation.upload;

import com.waynehays.cloudfilestorage.core.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceAlreadyExistsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidateStepTest extends BaseUploadStepTest {

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private ValidateStep validateStep;

    @Test
    @DisplayName("Should pass when no existing paths")
    void shouldPass_whenNoExistingPaths() {
        // given
        UploadContext context = uploadContext(
                uploadObject("key-1", "user/1/file1.txt", 100),
                uploadObject("key-2", "user/1/file2.txt", 200)
        );
        when(metadataService.findExistingPaths(any(), any())).thenReturn(Set.of());

        // when & then
        assertThatNoException().isThrownBy(() -> validateStep.execute(context));
    }

    @Test
    @DisplayName("Should throw when paths already exist in database")
    void shouldThrow_whenPathsAlreadyExistInDatabase() {
        // given
        UploadContext context = uploadContext(
                uploadObject("key-1", "user/1/file1.txt", 100)
        );
        when(metadataService.findExistingPaths(any(), any()))
                .thenReturn(Set.of("user/1/file1.txt"));

        // when & then
        assertThatThrownBy(() -> validateStep.execute(context))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }
}
