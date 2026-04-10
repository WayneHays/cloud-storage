package com.waynehays.cloudfilestorage.unit.service;

import com.waynehays.cloudfilestorage.dto.internal.metadata.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.entity.ResourceType;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.mapper.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.service.directory.DirectoryService;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DirectoryServiceTest {

    @Mock
    private ResourceDtoMapper mapper;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private DirectoryService directoryService;

    private static final Long USER_ID = 1L;

    @Test
    void getContent_shouldReturnMappedResources() {
        // given
        ResourceMetadataDto file = new ResourceMetadataDto(
                1L, USER_ID, "docs/file.txt", "docs/", "file.txt",
                100L, ResourceType.FILE);
        ResourceMetadataDto dir = new ResourceMetadataDto(
                2L, USER_ID, "docs/sub/", "docs/", "sub",
                null, ResourceType.DIRECTORY);
        ResourceDto fileDto = new ResourceDto("docs/", "file.txt", 100L, ResourceType.FILE);
        ResourceDto dirDto = new ResourceDto("docs/", "sub/", null, ResourceType.DIRECTORY);

        when(metadataService.findDirectoryContent(USER_ID, "docs/"))
                .thenReturn(List.of(file, dir));
        when(mapper.fromResourceMetadataDto(file)).thenReturn(fileDto);
        when(mapper.fromResourceMetadataDto(dir)).thenReturn(dirDto);

        // when
        List<ResourceDto> result = directoryService.getContent(USER_ID, "docs/");

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("file.txt");
        assertThat(result.get(0).type()).isEqualTo(ResourceType.FILE);
        assertThat(result.get(1).name()).isEqualTo("sub/");
        assertThat(result.get(1).type()).isEqualTo(ResourceType.DIRECTORY);
    }

    @Test
    void getContent_shouldReturnEmptyListForEmptyDirectory() {
        // given
        when(metadataService.findDirectoryContent(USER_ID, "empty/"))
                .thenReturn(List.of());

        // when
        List<ResourceDto> result = directoryService.getContent(USER_ID, "empty/");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void createDirectory_shouldSaveAndReturnDto() {
        // given
        String path = "docs/reports/";
        ResourceDto expected = new ResourceDto("docs/", "reports/", null, ResourceType.DIRECTORY);

        when(metadataService.findExistingPaths(eq(USER_ID), anySet()))
                .thenReturn(Set.of("docs/"));
        when(mapper.directoryFromPath(path)).thenReturn(expected);

        // when
        ResourceDto result = directoryService.createDirectory(USER_ID, path);

        // then
        verify(metadataService).saveDirectory(USER_ID, path);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void createDirectory_shouldThrowWhenDirectoryAlreadyExists() {
        // given
        String path = "docs/";
        when(metadataService.findExistingPaths(eq(USER_ID), anySet()))
                .thenReturn(Set.of("docs/"));

        // when & then
        assertThatThrownBy(() -> directoryService.createDirectory(USER_ID, path))
                .isInstanceOf(ResourceAlreadyExistsException.class);
        verify(metadataService, never()).saveDirectory(anyLong(), anyString());
    }

    @Test
    void createDirectory_shouldThrowWhenParentDirectoryNotFound() {
        // given
        String path = "docs/reports/";
        when(metadataService.findExistingPaths(eq(USER_ID), anySet()))
                .thenReturn(Set.of());

        // when & then
        assertThatThrownBy(() -> directoryService.createDirectory(USER_ID, path))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(metadataService, never()).saveDirectory(anyLong(), anyString());
    }

    @Test
    void createDirectory_shouldAllowRootLevelDirectoryWithoutParentCheck() {
        // given
        String path = "docs/";
        ResourceDto expected = new ResourceDto("", "docs/", null, ResourceType.DIRECTORY);

        when(metadataService.findExistingPaths(eq(USER_ID), anySet()))
                .thenReturn(Set.of());
        when(mapper.directoryFromPath(path)).thenReturn(expected);

        // when
        ResourceDto result = directoryService.createDirectory(USER_ID, path);

        // then
        verify(metadataService).saveDirectory(USER_ID, path);
        assertThat(result).isEqualTo(expected);
    }
}
