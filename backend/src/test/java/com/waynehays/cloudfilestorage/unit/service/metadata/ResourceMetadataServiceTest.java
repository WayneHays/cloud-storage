package com.waynehays.cloudfilestorage.unit.service.metadata;

import com.waynehays.cloudfilestorage.dto.internal.metadata.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.entity.ResourceType;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.mapper.ResourceMetadataMapper;
import com.waynehays.cloudfilestorage.repository.metadata.ResourceMetadataRepository;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceMetadataService unit tests")
class ResourceMetadataServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private ResourceMetadataMapper mapper;

    @Mock
    private ResourceMetadataRepository repository;

    @InjectMocks
    private ResourceMetadataService service;

    @Nested
    @DisplayName("findOrThrow")
    class FindOrThrow {

        @Test
        @DisplayName("should return dto when resource found")
        void shouldReturnDtoWhenFound() {
            // given
            ResourceMetadata entity = new ResourceMetadata();
            entity.setId(1L);
            ResourceMetadataDto dto = new ResourceMetadataDto(
                    1L, USER_ID, "docs/file.txt", "docs/", "file.txt",
                    100L, ResourceType.FILE);

            when(repository.findByPath(USER_ID, "docs/file.txt"))
                    .thenReturn(Optional.of(entity));
            when(mapper.toResourceMetadataDto(entity)).thenReturn(dto);

            // when
            ResourceMetadataDto result = service.findOrThrow(USER_ID, "docs/file.txt");

            // then
            assertThat(result).isEqualTo(dto);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when resource missing")
        void shouldThrowWhenNotFound() {
            // given
            when(repository.findByPath(USER_ID, "missing.txt"))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.findOrThrow(USER_ID, "missing.txt"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findDirectoryContent")
    class FindDirectoryContent {

        @Test
        @DisplayName("should verify directory existence and return its content")
        void shouldVerifyExistenceAndReturnContent() {
            // given
            ResourceMetadata dirEntity = new ResourceMetadata();
            ResourceMetadataDto dirDto = new ResourceMetadataDto(
                    1L, USER_ID, "docs/", "", "docs",
                    null, ResourceType.DIRECTORY);

            ResourceMetadata fileEntity = new ResourceMetadata();
            ResourceMetadataDto fileDto = new ResourceMetadataDto(
                    2L, USER_ID, "docs/file.txt", "docs/", "file.txt",
                    100L, ResourceType.FILE);

            when(repository.findByPath(USER_ID, "docs/"))
                    .thenReturn(Optional.of(dirEntity));
            when(mapper.toResourceMetadataDto(dirEntity)).thenReturn(dirDto);
            when(repository.findByParentPath(USER_ID, "docs/"))
                    .thenReturn(List.of(fileEntity));
            when(mapper.toResourceMetadataDto(List.of(fileEntity)))
                    .thenReturn(List.of(fileDto));

            // when
            List<ResourceMetadataDto> result = service.findDirectoryContent(USER_ID, "docs/");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().name()).isEqualTo("file.txt");
            verify(repository).findByPath(USER_ID, "docs/");
        }

        @Test
        @DisplayName("should skip existence check for root path")
        void shouldSkipExistenceCheckForRoot() {
            // given
            when(repository.findByParentPath(USER_ID, ""))
                    .thenReturn(List.of());
            when(mapper.toResourceMetadataDto(List.of()))
                    .thenReturn(List.of());

            // when
            List<ResourceMetadataDto> result = service.findDirectoryContent(USER_ID, "");

            // then
            assertThat(result).isEmpty();
            verify(repository, never()).findByPath(anyLong(), anyString());
        }
    }

    @Nested
    @DisplayName("saveDirectory")
    class SaveDirectory {

        @Test
        @DisplayName("should map to entity and save via repository")
        void shouldMapAndSave() {
            // given
            ResourceMetadata entity = new ResourceMetadata();
            when(mapper.toDirectoryEntity(USER_ID, "docs/")).thenReturn(entity);

            // when
            service.saveDirectory(USER_ID, "docs/");

            // then
            verify(repository).saveAndFlush(entity);
        }

        @Test
        @DisplayName("should throw ResourceAlreadyExistsException on duplicate path")
        void shouldThrowOnDuplicate() {
            // given
            ResourceMetadata directory = new ResourceMetadata();
            when(mapper.toDirectoryEntity(USER_ID, "docs/")).thenReturn(directory);
            when(repository.saveAndFlush(directory))
                    .thenThrow(new DataIntegrityViolationException("Duplicate"));

            // when & then
            assertThatThrownBy(() -> service.saveDirectory(USER_ID, "docs/"))
                    .isInstanceOf(ResourceAlreadyExistsException.class);
        }
    }
}
