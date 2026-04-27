package com.waynehays.cloudfilestorage.files.operation.move;

import com.waynehays.cloudfilestorage.core.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.infrastructure.storage.ResourceStorageException;
import com.waynehays.cloudfilestorage.infrastructure.storage.ResourceStorageServiceApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoveStorageStepTest extends BaseMoveStepTest{

    @Mock
    private ResourceStorageServiceApi storageService;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    private MoveStorageStep step;

    @BeforeEach
    void setUp() {
        step = new MoveStorageStep(storageService, metadataService, Executors.newSingleThreadExecutor());
    }

    @Test
    @DisplayName("Should move file in storage")
    void shouldMoveFileInStorage() {
        // given
        MoveContext context = fileContext("docs/file.txt", "images/file.txt");

        // when
        step.execute(context);

        // then
        verify(storageService).moveObject(USER_ID, "docs/file.txt", "images/file.txt");
    }

    @Test
    @DisplayName("Should move all files in directory async")
    void shouldMoveAllFilesInDirectory_async() {
        // given
        MoveContext context = directoryContext("docs/", "images/");
        List<ResourceMetadataDto> files = List.of(
                fileMetadata(2L, "docs/a.txt", 100L),
                fileMetadata(3L, "docs/b.txt", 200L)
        );
        when(metadataService.findFilesByPathPrefix(USER_ID, "docs/")).thenReturn(files);

        // when
        step.execute(context);

        // then
        verify(storageService).moveObject(USER_ID, "docs/a.txt", "images/a.txt");
        verify(storageService).moveObject(USER_ID, "docs/b.txt", "images/b.txt");
    }

    @Test
    @DisplayName("Should throw when storage fails during directory move")
    void shouldThrow_whenStorageFailsDuringDirectoryMove() {
        // given
        MoveContext context = directoryContext("docs/", "images/");
        when(metadataService.findFilesByPathPrefix(USER_ID, "docs/"))
                .thenReturn(List.of(fileMetadata(2L, "docs/file.txt", 100L)));
        doThrow(new ResourceStorageException("MinIO error"))
                .when(storageService).moveObject(USER_ID, "docs/file.txt", "images/file.txt");

        // when & then
        assertThatThrownBy(() -> step.execute(context))
                .isInstanceOf(ResourceStorageException.class);
    }

    @Test
    @DisplayName("Should rollback moves in reverse order")
    void shouldReverseMoves_inReverseOrder() {
        // given
        MoveRollbackDto snapshot = new MoveRollbackDto(
                USER_ID,
                List.of(new MovedObject("docs/a.txt", "images/a.txt"),
                        new MovedObject("docs/b.txt", "images/b.txt")));

        // when
        step.rollback(snapshot);

        // then
        verify(storageService).moveObject(USER_ID, "images/a.txt", "docs/a.txt");
        verify(storageService).moveObject(USER_ID, "images/b.txt", "docs/b.txt");
    }

    @Test
    @DisplayName("Should do nothing when nothing was moved")
    void shouldDoNothing_whenNothingWasMoved() {
        // given
        MoveRollbackDto snapshot = new MoveRollbackDto(USER_ID, List.of());

        // when
        step.rollback(snapshot);

        // then
        verifyNoInteractions(storageService);
    }
}
