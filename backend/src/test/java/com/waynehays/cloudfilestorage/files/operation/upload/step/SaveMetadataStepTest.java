package com.waynehays.cloudfilestorage.files.operation.upload.step;

import com.waynehays.cloudfilestorage.core.metadata.dto.CreateFileDto;
import com.waynehays.cloudfilestorage.core.metadata.service.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.files.operation.upload.dto.Context;
import com.waynehays.cloudfilestorage.files.operation.upload.dto.RollbackDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SaveMetadataStepTest extends BaseUploadStepTest {

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private SaveMetadataStep step;

    @Test
    @DisplayName("Should save files with CreateFileDtos built from context objects and register paths for rollback")
    void shouldSaveFilesAndRegisterPathsForRollback() {
        // given
        Context context = uploadContext(
                uploadObject("key-1", "user/1/file1.txt", 100),
                uploadObject("key-2", "user/1/file2.txt", 200)
        );

        // when
        step.execute(context);

        // then
        verify(metadataService).saveFiles(eq(USER_ID), argThat((List<CreateFileDto> files) ->
                files.stream().anyMatch(f -> f.storageKey().equals("key-1") && f.path().equals("user/1/file1.txt") && f.size() == 100)
                && files.stream().anyMatch(f -> f.storageKey().equals("key-2") && f.path().equals("user/1/file2.txt") && f.size() == 200)
        ));
        assertThat(context.rollbackDto().savedToDbPaths())
                .containsExactlyInAnyOrder("user/1/file1.txt", "user/1/file2.txt");
    }

    @Test
    @DisplayName("Should delete saved paths on rollback")
    void shouldDeleteSavedPaths_whenPathsExist() {
        // given
        RollbackDto snapshot = new RollbackDto(
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