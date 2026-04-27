package com.waynehays.cloudfilestorage.files.operation.move;

import com.waynehays.cloudfilestorage.core.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceAlreadyExistsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidateMoveStepTest extends BaseMoveStepTest {

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private ValidateMoveStep step;

    @Test
    @DisplayName("Should pass when paths are valid and target not exists")
    void shouldPass_whenPathsAreValidAndTargetDoesNotExist() {
        // given
        MoveContext context = fileContext("docs/file.txt", "images/file.txt");
        when(metadataService.existsByPath(USER_ID, "images/file.txt")).thenReturn(false);

        // when & then
        assertThatNoException().isThrownBy(() -> step.execute(context));
    }

    @Test
    @DisplayName("Should throw when moving directory to file")
    void shouldThrow_whenMovingDirectoryToFilePath() {
        // given
        MoveContext context = directoryContext("docs/", "file.txt");

        // when & then
        assertThatThrownBy(() -> step.execute(context))
                .isInstanceOf(InvalidMoveException.class);
    }

    @Test
    @DisplayName("Should throw when moving directory into itself")
    void shouldThrow_whenMovingDirectoryIntoItself() {
        // given
        MoveContext context = directoryContext("docs/", "docs/sub/");

        // when & then
        assertThatThrownBy(() -> step.execute(context))
                .isInstanceOf(InvalidMoveException.class);
    }

    @Test
    @DisplayName("Should throw when target path already exists")
    void shouldThrow_whenTargetPathAlreadyExists() {
        // given
        MoveContext context = fileContext("docs/file.txt", "images/file.txt");
        when(metadataService.existsByPath(USER_ID, "images/file.txt")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> step.execute(context))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }
}
