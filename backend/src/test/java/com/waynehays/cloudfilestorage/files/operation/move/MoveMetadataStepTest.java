package com.waynehays.cloudfilestorage.files.operation.move;

import com.waynehays.cloudfilestorage.core.metadata.ResourceMetadataServiceApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MoveMetadataStepTest extends BaseMoveStepTest {

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private MoveMetadataStep step;

    @Test
    @DisplayName("Should update file path in database")
    void shouldUpdateFilePathInDatabase() {
        // given
        MoveContext context = fileContext("docs/file.txt", "images/file.txt");

        // when
        step.execute(context);

        // then
        verify(metadataService).moveMetadata(USER_ID, "docs/file.txt", "images/file.txt");
    }

    @Test
    @DisplayName("Should update directory path in database")
    void shouldUpdateDirectoryPathInDatabase() {
        // given
        MoveContext context = directoryContext("docs/", "images/");

        // when
        step.execute(context);

        // then
        verify(metadataService).moveMetadata(USER_ID, "docs/", "images/");
    }
}
