package com.waynehays.cloudfilestorage.files.operation.move;

import com.waynehays.cloudfilestorage.core.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.waynehays.cloudfilestorage.files.operation.move.MoveTestHelper.USER_ID;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidateMoveStepTest {

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private ValidateMoveStep validateMoveStep;

    @Test
    void execute_shouldPass_whenPathsAreValidAndTargetDoesNotExist() {
        // given
        MoveContext context = MoveTestHelper.fileContext("docs/file.txt", "images/file.txt");
        when(metadataService.existsByPath(USER_ID, "images/file.txt")).thenReturn(false);

        // when & then
        assertThatNoException().isThrownBy(() -> validateMoveStep.execute(context));
    }

    @Test
    void execute_shouldThrow_whenMovingDirectoryToFilePath() {
        // given
        MoveContext context = MoveTestHelper.directoryContext("docs/", "file.txt");

        // when & then
        assertThatThrownBy(() -> validateMoveStep.execute(context))
                .isInstanceOf(InvalidMoveException.class);
    }

    @Test
    void execute_shouldThrow_whenMovingDirectoryIntoItself() {
        // given
        MoveContext context = MoveTestHelper.directoryContext("docs/", "docs/sub/");

        // when & then
        assertThatThrownBy(() -> validateMoveStep.execute(context))
                .isInstanceOf(InvalidMoveException.class);
    }

    @Test
    void execute_shouldThrow_whenTargetPathAlreadyExists() {
        // given
        MoveContext context = MoveTestHelper.fileContext("docs/file.txt", "images/file.txt");
        when(metadataService.existsByPath(USER_ID, "images/file.txt")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> validateMoveStep.execute(context))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }
}
