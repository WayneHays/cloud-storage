package com.waynehays.cloudfilestorage.service.resource.upload.refactor;

import com.waynehays.cloudfilestorage.dto.internal.metadata.FileRowDto;
import com.waynehays.cloudfilestorage.mapper.BatchInsertMapper;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.resource.upload.RollbackSnapshot;
import com.waynehays.cloudfilestorage.service.resource.upload.SaveMetadataStep;
import com.waynehays.cloudfilestorage.service.resource.upload.UploadContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveMetadataStepTest {

    @Mock
    private BatchInsertMapper batchInsertMapper;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private SaveMetadataStep saveMetadataStep;

    @Test
    void execute_shouldSaveFilesAndRegisterPathsForRollback() {
        // given
        UploadContext context = UploadTestHelper.uploadContext(1L,
                UploadTestHelper.uploadObject("user/1/file1.txt", 100),
                UploadTestHelper.uploadObject("user/1/file2.txt", 200)
        );
        List<FileRowDto> fileRows = List.of(
                UploadTestHelper.fileRowDto("user/1/file1.txt", "file1.txt"),
                UploadTestHelper.fileRowDto("user/1/file2.txt", "file2.txt")
        );
        when(batchInsertMapper.toFileRows(context.getObjects())).thenReturn(fileRows);

        // when
        saveMetadataStep.execute(context);

        // then
        verify(metadataService).saveFiles(1L, fileRows);
        assertThat(context.rollbackSnapshot().savedToDbPaths())
                .containsExactlyInAnyOrder("user/1/file1.txt", "user/1/file2.txt");
    }

    @Test
    void rollback_shouldDeleteSavedPaths_whenPathsExist() {
        // given
        RollbackSnapshot snapshot = new RollbackSnapshot(
                1L, 0L, false,
                List.of(),
                List.of("user/1/file1.txt", "user/1/file2.txt")
        );

        // when
        saveMetadataStep.rollback(snapshot);

        // then
        verify(metadataService).deleteByPaths(1L, List.of("user/1/file1.txt", "user/1/file2.txt"));
    }

    @Test
    void rollback_shouldDoNothing_whenNoSavedPaths() {
        // given
        RollbackSnapshot snapshot = new RollbackSnapshot(1L, 0L, false, List.of(), List.of());

        // when
        saveMetadataStep.rollback(snapshot);

        // then
        verifyNoInteractions(metadataService);
    }
}
