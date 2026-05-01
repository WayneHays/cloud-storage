package com.waynehays.cloudfilestorage.files.operation.upload;

import com.waynehays.cloudfilestorage.core.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.core.metadata.dto.FileRowDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveMetadataStepTest extends BaseUploadStepTest{

    @Mock
    private BatchInsertMapper batchInsertMapper;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private SaveMetadataStep step;

    @Test
    @DisplayName("Should save files and register paths for rollback")
    void shouldSaveFilesAndRegisterPathsForRollback() {
        // given
        UploadContext context = uploadContext(
                uploadObject("user/1/file1.txt", 100),
                uploadObject("user/1/file2.txt", 200)
        );
        List<FileRowDto> fileRows = List.of(
                fileRowDto("user/1/file1.txt", "file1.txt"),
                fileRowDto("user/1/file2.txt", "file2.txt")
        );
        when(batchInsertMapper.toFileRows(context.getObjects())).thenReturn(fileRows);

        // when
        step.execute(context);

        // then
        verify(metadataService).saveFiles(USER_ID, fileRows);
        assertThat(context.rollbackSnapshot().savedToDbPaths())
                .containsExactlyInAnyOrder("user/1/file1.txt", "user/1/file2.txt");
    }

    @Test
    @DisplayName("Should delete saved paths when paths exists")
    void shouldDeleteSavedPaths_whenPathsExist() {
        // given
        UploadRollbackDto snapshot = new UploadRollbackDto(
                USER_ID, 0L, false,
                List.of(),
                List.of("user/1/file1.txt", "user/1/file2.txt")
        );

        // when
        step.rollback(snapshot);

        // then
        verify(metadataService).deleteByPaths(USER_ID, List.of("user/1/file1.txt", "user/1/file2.txt"));
    }
}
