package com.waynehays.cloudfilestorage.resource.service.moving;

import com.waynehays.cloudfilestorage.resource.dto.internal.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.resource.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.shared.exception.ResourceStorageOperationException;
import com.waynehays.cloudfilestorage.storage.service.ResourceStorageServiceApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.Executors;

import static com.waynehays.cloudfilestorage.resource.service.moving.MoveTestHelper.USER_ID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoveStorageStepTest {

    @Mock
    private ResourceStorageServiceApi storageService;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    private MoveStorageStep moveStorageStep;

    @BeforeEach
    void setUp() {
        moveStorageStep = new MoveStorageStep(storageService, metadataService, Executors.newSingleThreadExecutor());
    }

    @Test
    void execute_shouldMoveFileInStorage() {
        // given
        MoveContext context = MoveTestHelper.fileContext("docs/file.txt", "images/file.txt");

        // when
        moveStorageStep.execute(context);

        // then
        verify(storageService).moveObject(USER_ID, "docs/file.txt", "images/file.txt");
    }

    @Test
    void execute_shouldMoveAllFilesInDirectory_async() {
        // given
        MoveContext context = MoveTestHelper.directoryContext("docs/", "images/");
        List<ResourceMetadataDto> files = List.of(
                MoveTestHelper.fileMetadata(2L, "docs/a.txt", 100L),
                MoveTestHelper.fileMetadata(3L, "docs/b.txt", 200L)
        );
        when(metadataService.findFilesByPathPrefix(USER_ID, "docs/")).thenReturn(files);

        // when
        moveStorageStep.execute(context);

        // then
        verify(storageService).moveObject(USER_ID, "docs/a.txt", "images/a.txt");
        verify(storageService).moveObject(USER_ID, "docs/b.txt", "images/b.txt");
    }

    @Test
    void execute_shouldThrow_whenStorageFailsDuringDirectoryMove() {
        // given
        MoveContext context = MoveTestHelper.directoryContext("docs/", "images/");
        when(metadataService.findFilesByPathPrefix(USER_ID, "docs/"))
                .thenReturn(List.of(MoveTestHelper.fileMetadata(2L, "docs/file.txt", 100L)));
        doThrow(new ResourceStorageOperationException("MinIO error"))
                .when(storageService).moveObject(USER_ID, "docs/file.txt", "images/file.txt");

        // when & then
        assertThatThrownBy(() -> moveStorageStep.execute(context))
                .isInstanceOf(ResourceStorageOperationException.class);
    }

    @Test
    void rollback_shouldReverseMoves_inReverseOrder() {
        // given
        MoveRollbackSnapshot snapshot = new MoveRollbackSnapshot(
                USER_ID,
                List.of(new MovedObject("docs/a.txt", "images/a.txt"),
                        new MovedObject("docs/b.txt", "images/b.txt")));

        // when
        moveStorageStep.rollback(snapshot);

        // then
        verify(storageService).moveObject(USER_ID, "images/a.txt", "docs/a.txt");
        verify(storageService).moveObject(USER_ID, "images/b.txt", "docs/b.txt");
    }

    @Test
    void rollback_shouldDoNothing_whenNothingWasMoved() {
        // given
        MoveRollbackSnapshot snapshot = new MoveRollbackSnapshot(USER_ID, List.of());

        // when
        moveStorageStep.rollback(snapshot);

        // then
        verifyNoInteractions(storageService);
    }
}
