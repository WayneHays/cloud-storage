package com.waynehays.cloudfilestorage.files.operation.directory;

import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.core.metadata.entity.ResourceType;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.core.metadata.service.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.files.api.dto.response.ResourceResponse;
import com.waynehays.cloudfilestorage.files.api.support.ResourceResponseMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DirectoryServiceTest {

    @Mock
    private ResourceResponseMapper responseMapper;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private DirectoryService directoryService;

    private static final Long USER_ID = 1L;

    @Nested
    @DisplayName("getContent")
    class GetContent {

        @Test
        @DisplayName("should return mapped resources")
        void shouldReturnMappedResources() {
            // given
            ResourceMetadataDto file = new ResourceMetadataDto(
                    1L, USER_ID, "storage-key", "docs/file.txt", "docs/", "file.txt",
                    100L, ResourceType.FILE);
            ResourceMetadataDto dir = new ResourceMetadataDto(
                    2L, USER_ID, null, "docs/sub/", "docs/", "sub",
                    null, ResourceType.DIRECTORY);
            ResourceResponse fileDto = new ResourceResponse("docs/", "file.txt", 100L, ResourceType.FILE);
            ResourceResponse dirDto = new ResourceResponse("docs/", "sub/", null, ResourceType.DIRECTORY);

            when(metadataService.findDirectoryContent(USER_ID, "docs/"))
                    .thenReturn(List.of(file, dir));
            when(responseMapper.fromResourceMetadataDto(List.of(file, dir)))
                    .thenReturn(List.of(fileDto, dirDto));

            // when
            List<ResourceResponse> result = directoryService.getContent(USER_ID, "docs/");

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("file.txt");
            assertThat(result.get(0).type()).isEqualTo(ResourceType.FILE);
            assertThat(result.get(1).name()).isEqualTo("sub/");
            assertThat(result.get(1).type()).isEqualTo(ResourceType.DIRECTORY);
        }

        @Test
        @DisplayName("should return empty list for empty directory")
        void shouldReturnEmptyListForEmptyDirectory() {
            // given
            when(metadataService.findDirectoryContent(USER_ID, "empty/"))
                    .thenReturn(List.of());

            // when
            List<ResourceResponse> result = directoryService.getContent(USER_ID, "empty/");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createDirectory")
    class CreateDirectory {

        @Test
        @DisplayName("should save and return dto")
        void shouldSaveAndReturnDto() {
            // given
            String path = "docs/reports/";
            ResourceMetadataDto savedDto = new ResourceMetadataDto(
                    1L, USER_ID, null, path, "docs/", "reports", null, ResourceType.DIRECTORY);
            ResourceResponse expected = new ResourceResponse("docs/", "reports/", null, ResourceType.DIRECTORY);

            when(metadataService.existsByPath(USER_ID, "docs/")).thenReturn(true);
            when(metadataService.saveDirectory(USER_ID, path)).thenReturn(savedDto);
            when(responseMapper.toCreatedDirectoryResponse(savedDto)).thenReturn(expected);

            // when
            ResourceResponse result = directoryService.createDirectory(USER_ID, path);

            // then
            verify(metadataService).saveDirectory(USER_ID, path);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should throw exception when directory already exists")
        void shouldThrowWhenDirectoryAlreadyExists() {
            // given
            String path = "docs/";
            doThrow(new ResourceAlreadyExistsException("Directory already exists", path))
                    .when(metadataService).saveDirectory(USER_ID, path);

            // when & then
            assertThatThrownBy(() -> directoryService.createDirectory(USER_ID, path))
                    .isInstanceOf(ResourceAlreadyExistsException.class);
        }

        @Test
        @DisplayName("Should throw exception when parent directory not found")
        void shouldThrowWhenParentDirectoryNotFound() {
            // given
            String path = "docs/reports/";

            // when & then
            assertThatThrownBy(() -> directoryService.createDirectory(USER_ID, path))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(metadataService, never()).saveDirectory(anyLong(), anyString());
        }

        @Test
        @DisplayName("Should allow root level directory without parent check")
        void shouldAllowRootLevelDirectoryWithoutParentCheck() {
            // given
            String path = "docs/";
            ResourceMetadataDto savedDto = new ResourceMetadataDto(
                    1L, USER_ID, null, path, "", "docs", null, ResourceType.DIRECTORY);
            ResourceResponse expected = new ResourceResponse("", "docs/", null, ResourceType.DIRECTORY);

            when(metadataService.saveDirectory(USER_ID, path)).thenReturn(savedDto);
            when(responseMapper.toCreatedDirectoryResponse(savedDto)).thenReturn(expected);

            // when
            ResourceResponse result = directoryService.createDirectory(USER_ID, path);

            // then
            verify(metadataService).saveDirectory(USER_ID, path);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should throw when file with same name already exists")
        void shouldThrowWhenFileWithSameNameExists() {
            // given
            String path = "docs/";
            doThrow(new ResourceAlreadyExistsException(
                    "Resources with same name, but different type already exist", "docs"))
                    .when(metadataService).throwIfAnyConflictingTypeExists(USER_ID, List.of(path));

            // when & then
            assertThatThrownBy(() -> directoryService.createDirectory(USER_ID, path))
                    .isInstanceOf(ResourceAlreadyExistsException.class);
            verify(metadataService, never()).saveDirectory(anyLong(), anyString());
        }
    }
}