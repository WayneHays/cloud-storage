package com.waynehays.cloudfilestorage.core.metadata;

import com.waynehays.cloudfilestorage.core.metadata.dto.DeleteDirectoryResult;
import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceNotFoundException;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceMetadataServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private ResourceMetadataMapper mapper;

    @Mock
    private ResourceMetadataRepository repository;

    @InjectMocks
    private ResourceMetadataService service;

    @Nested
    class ExistsByPath {

        @Test
        @DisplayName("Should normalize path and delegate to repository")
        void shouldNormalizeAndDelegate() {
            // given
            when(repository.existsByNormalizedPath(USER_ID, "docs/file.txt"))
                    .thenReturn(true);

            // when
            boolean result = service.existsByPath(USER_ID, "Docs/File.txt");

            // then
            assertThat(result).isTrue();
            verify(repository).existsByNormalizedPath(USER_ID, "docs/file.txt");
        }
    }

    @Nested
    class FindOrThrow {

        @Test
        @DisplayName("Should normalize path and return dto when found")
        void shouldReturnDtoWhenFound() {
            // given
            ResourceMetadata entity = new ResourceMetadata();
            ResourceMetadataDto dto = new ResourceMetadataDto(
                    1L, USER_ID, "storage-key", "Docs/File.txt", "docs/", "File.txt",
                    100L, ResourceType.FILE);

            when(repository.findByNormalizedPath(USER_ID, "docs/file.txt"))
                    .thenReturn(Optional.of(entity));
            when(mapper.toResourceMetadataDto(entity)).thenReturn(dto);

            // when
            ResourceMetadataDto result = service.findOrThrow(USER_ID, "Docs/File.txt");

            // then
            assertThat(result).isEqualTo(dto);
            verify(repository).findByNormalizedPath(USER_ID, "docs/file.txt");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            // given
            when(repository.findByNormalizedPath(USER_ID, "missing.txt"))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.findOrThrow(USER_ID, "missing.txt"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class FindDirectoryContent {

        @Test
        @DisplayName("Should return content when directory has files")
        void shouldReturnContentWhenDirectoryHasFiles() {
            // given
            ResourceMetadata fileEntity = new ResourceMetadata();
            ResourceMetadataDto fileDto = new ResourceMetadataDto(
                    2L, USER_ID, "storage-key", "Docs/File.txt", "docs/", "File.txt",
                    100L, ResourceType.FILE);

            when(repository.findByParentPath(USER_ID, "docs/"))
                    .thenReturn(List.of(fileEntity));
            when(mapper.toResourceMetadataDto(List.of(fileEntity)))
                    .thenReturn(List.of(fileDto));

            // when
            List<ResourceMetadataDto> result = service.findDirectoryContent(USER_ID, "Docs/");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().name()).isEqualTo("File.txt");
        }

        @Test
        @DisplayName("Should skip existence check for root path")
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
            verify(repository, never()).findByNormalizedPath(anyLong(), anyString());
        }

        @Test
        @DisplayName("Should throw when directory does not exist")
        void shouldThrowWhenDirectoryNotFound() {
            // given
            when(repository.findByParentPath(USER_ID, "missing/"))
                    .thenReturn(List.of());
            when(repository.existsByNormalizedPath(USER_ID, "missing/"))
                    .thenReturn(false);

            // when & then
            assertThatThrownBy(() -> service.findDirectoryContent(USER_ID, "missing/"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should return empty list for existing empty directory")
        void shouldReturnEmptyListForExistingEmptyDirectory() {
            // given
            when(repository.findByParentPath(USER_ID, "empty/"))
                    .thenReturn(List.of());
            when(repository.existsByNormalizedPath(USER_ID, "empty/"))
                    .thenReturn(true);
            when(mapper.toResourceMetadataDto(List.of()))
                    .thenReturn(List.of());

            // when
            List<ResourceMetadataDto> result = service.findDirectoryContent(USER_ID, "empty/");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindExistingPaths {

        @Test
        @DisplayName("Should normalize all paths before querying")
        void shouldNormalizeAllPaths() {
            // given
            when(repository.findExistingPaths(USER_ID, Set.of("docs/file.txt", "work/report.txt")))
                    .thenReturn(Set.of("Docs/File.txt"));

            // when
            Set<String> result = service.findExistingPaths(USER_ID,
                    Set.of("Docs/File.txt", "Work/Report.txt"));

            // then
            assertThat(result).containsExactly("Docs/File.txt");
            verify(repository).findExistingPaths(USER_ID,
                    Set.of("docs/file.txt", "work/report.txt"));
        }
    }

    @Test
    @DisplayName("Should return paths preserving original case")
    void shouldPreserveOriginalCase() {
        // given
        Set<String> paths = Set.of("Docs/", "Photos/Vacation/");
        when(repository.findMissingPaths(USER_ID, paths))
                .thenReturn(Set.of("Docs/", "Photos/Vacation/"));

        // when
        Set<String> result = service.findMissingPaths(USER_ID, paths);

        // then
        assertThat(result).containsExactlyInAnyOrder("Docs/", "Photos/Vacation/");
        verify(repository).findMissingPaths(USER_ID, paths);
    }

    @Nested
    class SaveDirectory {

        @Test
        @DisplayName("Should map to entity and save")
        void shouldMapAndSave() {
            // given
            ResourceMetadata entity = new ResourceMetadata();
            when(mapper.toDirectoryEntity(USER_ID, "Docs/")).thenReturn(entity);

            // when
            service.saveDirectory(USER_ID, "Docs/");

            // then
            verify(repository).saveAndFlush(entity);
        }

        @Test
        @DisplayName("Should throw ResourceAlreadyExistsException on duplicate")
        void shouldThrowOnDuplicate() {
            // given
            ResourceMetadata entity = new ResourceMetadata();
            when(mapper.toDirectoryEntity(USER_ID, "Docs/")).thenReturn(entity);
            when(repository.saveAndFlush(entity))
                    .thenThrow(new DataIntegrityViolationException("Duplicate"));

            // when & then
            assertThatThrownBy(() -> service.saveDirectory(USER_ID, "Docs/"))
                    .isInstanceOf(ResourceAlreadyExistsException.class);
        }
    }

    @Nested
    class MoveMetadata {

        @Test
        @DisplayName("Should normalize pathFrom and compute target fields")
        void shouldNormalizeAndComputeTargetFields() {
            // given
            when(repository.moveMetadata(USER_ID, "docs/", "Archive/Docs/", "archive/", "Docs"))
                    .thenReturn(3);

            // when
            service.moveMetadata(USER_ID, "Docs/", "Archive/Docs/");

            // then
            verify(repository).moveMetadata(USER_ID, "docs/", "Archive/Docs/", "archive/", "Docs");
        }

        @Test
        @DisplayName("Should compute empty parent path when moving to root")
        void shouldComputeEmptyParentForRoot() {
            // given
            when(repository.moveMetadata(USER_ID, "archive/docs/", "Docs/", "", "Docs"))
                    .thenReturn(1);

            // when
            service.moveMetadata(USER_ID, "archive/docs/", "Docs/");

            // then
            verify(repository).moveMetadata(USER_ID, "archive/docs/", "Docs/", "", "Docs");
        }

        @Test
        @DisplayName("Should compute target name for file move")
        void shouldComputeTargetNameForFile() {
            // given
            when(repository.moveMetadata(USER_ID, "docs/file.txt", "work/Report.txt", "work/", "Report.txt"))
                    .thenReturn(1);

            // when
            service.moveMetadata(USER_ID, "docs/file.txt", "work/Report.txt");

            // then
            verify(repository).moveMetadata(USER_ID, "docs/file.txt", "work/Report.txt", "work/", "Report.txt");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when nothing moved")
        void shouldThrowWhenNothingMoved() {
            // given
            when(repository.moveMetadata(USER_ID, "nonexistent/", "somewhere/", "", "somewhere"))
                    .thenReturn(0);

            // when & then
            assertThatThrownBy(() -> service.moveMetadata(USER_ID, "nonexistent/", "somewhere/"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class MarkForDeletion {

        @Test
        @DisplayName("Should normalize path before marking")
        void shouldNormalizePath() {
            // when
            service.markForDeletion(USER_ID, "Docs/File.txt");

            // then
            verify(repository).markForDeletionByNormalizedPath(USER_ID, "docs/file.txt");
        }
    }

    @Nested
    class DeleteFileByPath {

        @Test
        @DisplayName("Should normalize path before deleting")
        void shouldNormalizePath() {
            // when
            service.deleteFileByPath(USER_ID, "Docs/File.txt");

            // then
            verify(repository).deleteFileByNormalizedPath(USER_ID, "docs/file.txt");
        }
    }

    @Nested
    class DeleteDirectoryMetadata {

        @Test
        @DisplayName("Should normalize prefix before deleting")
        void shouldNormalizePrefix() {
            // when
            service.deleteDirectoryMetadata(USER_ID, "Docs/");

            // then
            verify(repository).deleteByNormalizedPathPrefix(USER_ID, "docs/");
        }
    }

    @Nested
    class DeleteByPaths {

        @Test
        @DisplayName("Should normalize all paths before deleting")
        void shouldNormalizeAllPaths() {
            // when
            service.deleteByPaths(USER_ID, List.of("Docs/A.txt", "Work/B.txt"));

            // then
            verify(repository).deleteByNormalizedPaths(USER_ID,
                    List.of("docs/a.txt", "work/b.txt"));
        }
    }

    @Nested
    class MarkFilesForDeletionAndCollectKeys {

        @Test
        @DisplayName("Should normalize path and return result with total size and storage keys")
        void shouldNormalizeAndReturnResult() {
            // given
            DeleteDirectoryResult expected = new DeleteDirectoryResult(300L, List.of("key1", "key2"));
            when(repository.markFilesForDeletionAndCollectKeys(USER_ID, "docs/"))
                    .thenReturn(expected);

            // when
            DeleteDirectoryResult result = service.markDirectoryForDeletionAndCollectKeys(USER_ID, "Docs/");

            // then
            assertThat(result.totalSize()).isEqualTo(300L);
            assertThat(result.storageKeys()).containsExactly("key1", "key2");
            verify(repository).markFilesForDeletionAndCollectKeys(USER_ID, "docs/");
        }

        @Test
        @DisplayName("Should return zero size and empty keys when no files found")
        void shouldReturnEmptyWhenNoFiles() {
            // given
            DeleteDirectoryResult expected = new DeleteDirectoryResult(0L, List.of());
            when(repository.markFilesForDeletionAndCollectKeys(USER_ID, "empty/"))
                    .thenReturn(expected);

            // when
            DeleteDirectoryResult result = service.markDirectoryForDeletionAndCollectKeys(USER_ID, "empty/");

            // then
            assertThat(result.totalSize()).isEqualTo(0L);
            assertThat(result.storageKeys()).isEmpty();
        }
    }
}
