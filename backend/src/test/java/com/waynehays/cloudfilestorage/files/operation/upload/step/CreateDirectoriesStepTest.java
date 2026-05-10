package com.waynehays.cloudfilestorage.files.operation.upload.step;

import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.core.metadata.entity.ResourceType;
import com.waynehays.cloudfilestorage.core.metadata.service.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.files.api.dto.response.ResourceResponse;
import com.waynehays.cloudfilestorage.files.api.support.ResourceResponseMapper;
import com.waynehays.cloudfilestorage.files.operation.upload.dto.Context;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateDirectoriesStepTest extends BaseUploadStepTest {

    @Mock
    private ResourceResponseMapper resourceResponseMapper;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private CreateDirectoriesStep step;

    @Test
    @DisplayName("Should save ancestor directories and register paths for rollback")
    void shouldSaveAncestorDirectoriesAndRegisterPathsForRollback() {
        // given
        Context context = uploadContext(
                uploadObject("key-1", "docs/reports/file.txt", 100)
        );
        ResourceMetadataDto docsDto = new ResourceMetadataDto(
                1L, USER_ID, null, "docs/", "", "docs", null, ResourceType.DIRECTORY);
        ResourceMetadataDto reportsDto = new ResourceMetadataDto(
                2L, USER_ID, null, "docs/reports/", "docs/", "reports", null, ResourceType.DIRECTORY);
        List<ResourceMetadataDto> savedDtos = List.of(docsDto, reportsDto);

        ResourceResponse docsResponse = directoryDto("", "docs/");
        ResourceResponse reportsResponse = directoryDto("docs/", "reports/");

        when(metadataService.saveDirectories(eq(USER_ID), anySet())).thenReturn(savedDtos);
        when(resourceResponseMapper.fromResourceMetadataDto(savedDtos)).thenReturn(List.of(docsResponse, reportsResponse));

        // when
        step.execute(context);

        // then
        verify(metadataService).saveDirectories(eq(USER_ID), anySet());
        assertThat(context.rollbackDto().savedToDbPaths())
                .containsExactlyInAnyOrder("docs/", "docs/reports/");
        assertThat(context.getResult()).containsExactlyInAnyOrder(docsResponse, reportsResponse);
    }

    @Test
    @DisplayName("Should do nothing when context has no objects")
    void shouldDoNothingWhenContextHasNoObjects() {
        // given
        Context context = uploadContext();

        // when
        step.execute(context);

        // then
        verifyNoInteractions(metadataService);
        verifyNoInteractions(resourceResponseMapper);
    }

    @Test
    @DisplayName("Should save directories when file is at root level")
    void shouldSaveDirectoriesForRootLevelFile() {
        // given
        Context context = uploadContext(
                uploadObject("key-1", "docs/file.txt", 100)
        );
        ResourceMetadataDto docsDto = new ResourceMetadataDto(
                1L, USER_ID, null, "docs/", "", "docs", null, ResourceType.DIRECTORY);

        when(metadataService.saveDirectories(eq(USER_ID), anySet())).thenReturn(List.of(docsDto));
        when(resourceResponseMapper.fromResourceMetadataDto(List.of(docsDto))).thenReturn(List.of(directoryDto("", "docs/")));

        // when
        step.execute(context);

        // then
        verify(metadataService).saveDirectories(eq(USER_ID), eq(Set.of("docs/")));
        assertThat(context.rollbackDto().savedToDbPaths()).containsExactly("docs/");
    }
}