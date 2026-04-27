package com.waynehays.cloudfilestorage.files.operation.upload;

import com.waynehays.cloudfilestorage.core.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.core.metadata.dto.DirectoryRowDto;
import com.waynehays.cloudfilestorage.files.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.files.operation.ResourceDtoMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateDirectoriesStepTest extends BaseUploadStepTest {

    @Mock
    private BatchInsertMapper batchInsertMapper;

    @Mock
    private ResourceDtoMapper resourceDtoMapper;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private CreateDirectoriesStep step;

    @Test
    @DisplayName("Should create missing directories and register paths for rollback")
    void shouldCreateMissingDirectoriesAndRegisterPathsForRollback() {
        // given
        UploadContext context = uploadContext();
        ResourceDto file = fileDto("user/1/a/b/", "file.txt", 100);
        context.addResult(file);

        Set<String> missingPaths = Set.of("user/1/a/", "user/1/a/b/");
        List<DirectoryRowDto> directoryRows = List.of(
                directoryRowDto("user/1/a/", "a"),
                directoryRowDto("user/1/a/b/", "b")
        );
        List<ResourceDto> dirDtos = List.of(
                directoryDto("user/1/", "a"),
                directoryDto("user/1/a/", "b")
        );

        when(metadataService.findMissingPaths(eq(USER_ID), any()))
                .thenReturn(missingPaths);
        when(batchInsertMapper.toDirectoryRows(missingPaths))
                .thenReturn(directoryRows);
        when(resourceDtoMapper.directoriesFromPaths(missingPaths))
                .thenReturn(dirDtos);

        // when
        step.execute(context);

        // then
        verify(metadataService).saveDirectories(USER_ID, directoryRows);
        assertThat(context.rollbackSnapshot().savedToDbPaths())
                .containsExactlyInAnyOrder("user/1/a/", "user/1/a/b/");
        assertThat(context.getResult()).containsAll(dirDtos);
    }

    @Test
    @DisplayName("Should do nothing when result is empty")
    void shouldDoNothing_whenResultIsEmpty() {
        // given
        UploadContext context = uploadContext();

        // when
        step.execute(context);

        // then
        verifyNoInteractions(metadataService);
    }

    @Test
    @DisplayName("Should do nothing when all directories already exists")
    void shouldDoNothing_whenAllDirectoriesAlreadyExist() {
        // given
        UploadContext context = uploadContext();
        ResourceDto file = fileDto("user/1/a/", "file.txt", 100);
        context.addResult(file);

        when(metadataService.findMissingPaths(eq(USER_ID), any()))
                .thenReturn(Set.of());

        // when
        step.execute(context);

        // then
        verify(metadataService, never()).saveDirectories(any(), any());
    }

    @Test
    @DisplayName("Should create directories with original case names")
    void shouldCreateDirectoriesWithOriginalCaseNames() {
        // given
        UploadContext context = uploadContext();
        ResourceDto file = fileDto("MyDocs/Work/", "report.txt", 100L);
        context.addResult(file);

        when(metadataService.findMissingPaths(eq(USER_ID), anySet()))
                .thenReturn(Set.of("MyDocs/", "MyDocs/Work/"));

        DirectoryRowDto row1 = new DirectoryRowDto("MyDocs/", "mydocs/", "", "MyDocs");
        DirectoryRowDto row2 = new DirectoryRowDto("MyDocs/Work/", "mydocs/work/", "mydocs/", "Work");
        when(batchInsertMapper.toDirectoryRows(Set.of("MyDocs/", "MyDocs/Work/")))
                .thenReturn(List.of(row1, row2));

        // when
        step.execute(context);

        // then
        verify(metadataService).saveDirectories(eq(USER_ID), argThat(dirs ->
                dirs.stream().anyMatch(d -> d.name().equals("MyDocs"))
                && dirs.stream().anyMatch(d -> d.name().equals("Work"))
        ));
    }
}