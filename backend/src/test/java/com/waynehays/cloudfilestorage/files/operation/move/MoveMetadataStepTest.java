package com.waynehays.cloudfilestorage.files.operation.move;

import com.waynehays.cloudfilestorage.core.metadata.ResourceMetadataServiceApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.waynehays.cloudfilestorage.files.operation.move.MoveTestHelper.USER_ID;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MoveMetadataStepTest {

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private MoveMetadataStep moveMetadataStep;

    @Test
    void execute_shouldUpdateMetadataPathsInDatabase() {
        // given
        MoveContext context = MoveTestHelper.fileContext("docs/file.txt", "images/file.txt");

        // when
        moveMetadataStep.execute(context);

        // then
        verify(metadataService).moveMetadata(USER_ID, "docs/file.txt", "images/file.txt");
    }

    @Test
    void execute_shouldUpdateDirectoryMetadataPathsInDatabase() {
        // given
        MoveContext context = MoveTestHelper.directoryContext("docs/", "images/");

        // when
        moveMetadataStep.execute(context);

        // then
        verify(metadataService).moveMetadata(USER_ID, "docs/", "images/");
    }
}
