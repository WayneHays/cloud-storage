package com.waynehays.cloudfilestorage.files.operation.move;

import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.core.metadata.entity.ResourceType;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.core.metadata.service.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.files.api.dto.response.ResourceResponse;
import com.waynehays.cloudfilestorage.files.api.support.ResourceResponseMapper;
import com.waynehays.cloudfilestorage.files.operation.move.exception.InvalidMoveException;
import org.junit.jupiter.api.DisplayName;
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
class ResourceMoveServiceTest {
    private static final Long USER_ID = 1L;

    @Mock
    private ResourceResponseMapper responseMapper;

    @Mock
    private ResourceMetadataServiceApi metadataService;

    @InjectMocks
    private ResourceMoveService moveService;

    @Test
    @DisplayName("Should update metadata and return mapped result")
    void shouldUpdateMetadataAndReturnResult() {
        // given
        ResourceMetadataDto moved = new ResourceMetadataDto(
                1L, USER_ID, "key-123", "images/file.txt", "images/",
                "file.txt", 100L, ResourceType.FILE);
        ResourceResponse expected = new ResourceResponse("images/", "file.txt", 100L, ResourceType.FILE);

        when(metadataService.existsByPath(USER_ID, "images/")).thenReturn(true);
        when(metadataService.existsByPath(USER_ID, "images/file.txt")).thenReturn(false);
        when(metadataService.findByPath(USER_ID, "images/file.txt")).thenReturn(moved);
        when(responseMapper.fromResourceMetadataDto(moved)).thenReturn(expected);

        // when
        ResourceResponse result = moveService.move(USER_ID, "docs/file.txt", "images/file.txt");

        // then
        verify(metadataService).moveMetadata(USER_ID, "docs/file.txt", "images/file.txt");
        verify(metadataService).findByPath(USER_ID, "images/file.txt");
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should update metadata and return mapped result for directory")
    void shouldUpdateMetadataForDirectory() {
        // given
        ResourceMetadataDto movedDir = new ResourceMetadataDto(
                2L, USER_ID, null, "images/", "", "images", null, ResourceType.DIRECTORY);
        ResourceResponse expected = new ResourceResponse("", "images/", null, ResourceType.DIRECTORY);

        when(metadataService.existsByPath(USER_ID, "images/")).thenReturn(false);
        when(metadataService.findByPath(USER_ID, "images/")).thenReturn(movedDir);
        when(responseMapper.fromResourceMetadataDto(movedDir)).thenReturn(expected);

        // when
        ResourceResponse result = moveService.move(USER_ID, "docs/", "images/");

        // then
        verify(metadataService).moveMetadata(USER_ID, "docs/", "images/");
        verify(metadataService).findByPath(USER_ID, "images/");
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should throw when moving directory to a file path")
    void shouldThrowWhenMovingDirectoryToFile() {
        // when & then
        assertThatThrownBy(() -> moveService.move(USER_ID, "docs/", "file.txt"))
                .isInstanceOf(InvalidMoveException.class);
        verify(metadataService, never()).moveMetadata(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw when moving directory into itself")
    void shouldThrowWhenMovingDirectoryIntoItself() {
        // when & then
        assertThatThrownBy(() -> moveService.move(USER_ID, "docs/", "docs/sub/"))
                .isInstanceOf(InvalidMoveException.class);
        verify(metadataService, never()).moveMetadata(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw when target path already exists")
    void shouldThrowWhenTargetAlreadyExists() {
        // given
        when(metadataService.existsByPath(USER_ID, "images/")).thenReturn(true);
        when(metadataService.existsByPath(USER_ID, "images/file.txt")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> moveService.move(USER_ID, "docs/file.txt", "images/file.txt"))
                .isInstanceOf(ResourceAlreadyExistsException.class);
        verify(metadataService, never()).moveMetadata(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should propagate exception when moveMetadata fails")
    void shouldPropagateWhenMoveMetadataFails() {
        // given
        when(metadataService.existsByPath(USER_ID, "new.txt")).thenReturn(false);
        doThrow(new ResourceNotFoundException("Resource not found", "missing.txt"))
                .when(metadataService).moveMetadata(USER_ID, "missing.txt", "new.txt");

        // when & then
        assertThatThrownBy(() -> moveService.move(USER_ID, "missing.txt", "new.txt"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw when resource with same name but different type exists at target")
    void shouldThrowWhenConflictingTypeExistsAtTarget() {
        // given
        when(metadataService.existsByPath(USER_ID, "report")).thenReturn(false);
        doThrow(new ResourceAlreadyExistsException(
                "Resources with same name, but different type already exist", "report/"))
                .when(metadataService).throwIfAnyConflictingTypeExists(USER_ID, List.of("report"));

        // when & then
        assertThatThrownBy(() -> moveService.move(USER_ID, "docs/report", "report"))
                .isInstanceOf(ResourceAlreadyExistsException.class);
        verify(metadataService, never()).moveMetadata(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw when target parent directory does not exist")
    void shouldThrowWhenTargetParentNotFound() {
        // given
        when(metadataService.existsByPath(USER_ID, "nonexistent/")).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> moveService.move(USER_ID, "file.txt", "nonexistent/file.txt"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(metadataService, never()).moveMetadata(anyLong(), anyString(), anyString());
    }
}