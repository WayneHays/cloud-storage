package com.waynehays.cloudfilestorage.resource.service.uploading;

import com.waynehays.cloudfilestorage.resource.dto.internal.DirectoryRowDto;
import com.waynehays.cloudfilestorage.resource.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.resource.mapper.BatchInsertMapper;
import com.waynehays.cloudfilestorage.resource.mapper.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.resource.service.metadata.ResourceMetadataServiceApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateDirectoriesStepTest {

    @Mock
    private BatchInsertMapper batchInsertMapper;

    @Mock
    private ResourceDtoMapper resourceDtoMapper;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private CreateDirectoriesStep createDirectoriesStep;

    @Test
    void execute_shouldCreateMissingDirectoriesAndRegisterPathsForRollback() {
        // given
        UploadContext context = UploadTestHelper.uploadContext(1L);
        context.addResult(List.of(UploadTestHelper.fileDto("user/1/a/b/", "file.txt", 100)));

        Set<String> missingPaths = Set.of("user/1/a/", "user/1/a/b/");
        List<DirectoryRowDto> directoryRows = List.of(
                UploadTestHelper.dirRowDto("user/1/a/", "a"),
                UploadTestHelper.dirRowDto("user/1/a/b/", "b")
        );
        List<ResourceDto> dirDtos = List.of(
                UploadTestHelper.dirDto("user/1/", "a"),
                UploadTestHelper.dirDto("user/1/a/", "b")
        );

        when(metadataService.findMissingPaths(eq(1L), any())).thenReturn(missingPaths);
        when(batchInsertMapper.toDirectoryRows(missingPaths)).thenReturn(directoryRows);
        when(resourceDtoMapper.directoriesFromPaths(missingPaths)).thenReturn(dirDtos);

        // when
        createDirectoriesStep.execute(context);

        // then
        verify(metadataService).saveDirectories(1L, directoryRows);
        assertThat(context.rollbackSnapshot().savedToDbPaths())
                .containsExactlyInAnyOrder("user/1/a/", "user/1/a/b/");
        assertThat(context.getResult()).containsAll(dirDtos);
    }

    @Test
    void execute_shouldDoNothing_whenResultIsEmpty() {
        // given
        UploadContext context = UploadTestHelper.uploadContext(1L);

        // when
        createDirectoriesStep.execute(context);

        // then
        verifyNoInteractions(metadataService);
    }

    @Test
    void execute_shouldDoNothing_whenAllDirectoriesAlreadyExist() {
        // given
        UploadContext context = UploadTestHelper.uploadContext(1L);
        context.addResult(List.of(UploadTestHelper.fileDto("user/1/a/", "file.txt", 100)));

        when(metadataService.findMissingPaths(eq(1L), any())).thenReturn(Set.of());

        // when
        createDirectoriesStep.execute(context);

        // then
        verify(metadataService, never()).saveDirectories(any(), any());
    }
}