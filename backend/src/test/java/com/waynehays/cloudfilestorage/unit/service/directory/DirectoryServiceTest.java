package com.waynehays.cloudfilestorage.unit.service.directory;

import com.waynehays.cloudfilestorage.component.ResourceDtoConverter;
import com.waynehays.cloudfilestorage.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.service.directory.DirectoryService;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DirectoryServiceTest {

    @Mock
    private ResourceDtoConverter dtoConverter;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private DirectoryService directoryService;

    private static final Long USER_ID = 1L;

    @Nested
    class GetContent {

        @Test
        void shouldReturnConvertedDtosForDirectChildren() {
            // given
            String path = "folder1/";
            ResourceMetadataDto metadata1 = mock(ResourceMetadataDto.class);
            ResourceMetadataDto metadata2 = mock(ResourceMetadataDto.class);
            ResourceDto dto1 = mock(ResourceDto.class);
            ResourceDto dto2 = mock(ResourceDto.class);

            when(metadataService.findDirectChildren(USER_ID, path))
                    .thenReturn(List.of(metadata1, metadata2));
            when(dtoConverter.fromMetadata(metadata1)).thenReturn(dto1);
            when(dtoConverter.fromMetadata(metadata2)).thenReturn(dto2);

            // when
            List<ResourceDto> result = directoryService.getContent(USER_ID, path);

            // then
            assertThat(result).containsExactly(dto1, dto2);
        }

        @Test
        void shouldReturnEmptyListWhenNoChildren() {
            // given
            String path = "empty-folder/";

            when(metadataService.findDirectChildren(USER_ID, path))
                    .thenReturn(List.of());

            // when
            List<ResourceDto> result = directoryService.getContent(USER_ID, path);

            // then
            assertThat(result).isEmpty();
            verifyNoInteractions(dtoConverter);
        }
    }

    @Nested
    class CreateDirectory {

        @Test
        void shouldValidateThenSaveThenReturnDto() {
            // given
            String path = "new-folder/";
            ResourceDto expectedDto = mock(ResourceDto.class);

            when(dtoConverter.directoryFromPath(path)).thenReturn(expectedDto);

            // when
            ResourceDto result = directoryService.createDirectory(USER_ID, path);

            // then
            InOrder inOrder = inOrder(metadataService, dtoConverter);
            inOrder.verify(metadataService).validateDirectoryCreation(USER_ID, path);
            inOrder.verify(metadataService).saveDirectories(USER_ID, Set.of(path));
            inOrder.verify(dtoConverter).directoryFromPath(path);

            assertThat(result).isEqualTo(expectedDto);
        }

        @Test
        void shouldPropagateExceptionFromValidation() {
            // given
            String path = "existing-folder/";

            doThrow(new RuntimeException("Directory already exists"))
                    .when(metadataService).validateDirectoryCreation(USER_ID, path);

            // when & then
            org.junit.jupiter.api.Assertions.assertThrows(
                    RuntimeException.class,
                    () -> directoryService.createDirectory(USER_ID, path)
            );

            verify(metadataService, never()).saveDirectories(any(), any());
            verifyNoInteractions(dtoConverter);
        }
    }
}