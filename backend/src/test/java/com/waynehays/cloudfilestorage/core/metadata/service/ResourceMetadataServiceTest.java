package com.waynehays.cloudfilestorage.core.metadata.service;

import com.waynehays.cloudfilestorage.core.metadata.dto.CreateFileDto;
import com.waynehays.cloudfilestorage.core.metadata.dto.DeleteDirectoryResult;
import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.core.metadata.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.core.metadata.entity.ResourceType;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.core.metadata.factory.ResourceMetadataFactory;
import com.waynehays.cloudfilestorage.core.metadata.mapper.ResourceMetadataMapper;
import com.waynehays.cloudfilestorage.core.metadata.repository.ResourceMetadataRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceMetadataServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private ResourceMetadataMapper mapper;

    @Mock
    private ResourceMetadataFactory factory;

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
    class FindByPath {

        @Test
        @DisplayName("Should normalize path and return dto when found")
        void shouldReturnDtoWhenFound() {
            // given
            ResourceMetadata entityMock = mock(ResourceMetadata.class);
            ResourceMetadataDto dto = new ResourceMetadataDto(
                    1L, USER_ID, "storage-key", "Docs/File.txt", "docs/", "File.txt",
                    100L, ResourceType.FILE);

            when(repository.findByNormalizedPath(USER_ID, "docs/file.txt"))
                    .thenReturn(Optional.of(entityMock));
            when(mapper.toDto(entityMock)).thenReturn(dto);

            // when
            ResourceMetadataDto result = service.findByPath(USER_ID, "Docs/File.txt");

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
            assertThatThrownBy(() -> service.findByPath(USER_ID, "missing.txt"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class FindDirectoryContent {

        @Test
        @DisplayName("Should return content when directory has files")
        void shouldReturnContentWhenDirectoryHasFiles() {
            // given
            ResourceMetadata mockEntity = mock(ResourceMetadata.class);
            ResourceMetadataDto fileDto = new ResourceMetadataDto(
                    2L, USER_ID, "storage-key", "Docs/File.txt", "docs/", "File.txt",
                    100L, ResourceType.FILE);

            when(repository.findByParentPath(USER_ID, "docs/"))
                    .thenReturn(List.of(mockEntity));
            when(mapper.toDto(List.of(mockEntity)))
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
            when(mapper.toDto(List.of()))
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
            when(mapper.toDto(List.of()))
                    .thenReturn(List.of());

            // when
            List<ResourceMetadataDto> result = service.findDirectoryContent(USER_ID, "empty/");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindFilesByPathPrefix {

        @Test
        @DisplayName("Should normalize prefix and return mapped file list")
        void shouldNormalizeAndReturnFiles() {
            // given
            ResourceMetadata mockEntity = mock(ResourceMetadata.class);
            ResourceMetadataDto fileDto = new ResourceMetadataDto(
                    1L, USER_ID, "storage-key", "Docs/File.txt", "docs/", "File.txt",
                    100L, ResourceType.FILE);

            when(repository.findFilesByPathPrefix(USER_ID, "docs/"))
                    .thenReturn(List.of(mockEntity));
            when(mapper.toDto(List.of(mockEntity)))
                    .thenReturn(List.of(fileDto));

            // when
            List<ResourceMetadataDto> result = service.findFilesByPathPrefix(USER_ID, "Docs/");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().name()).isEqualTo("File.txt");
            verify(repository).findFilesByPathPrefix(USER_ID, "docs/");
        }

        @Test
        @DisplayName("Should return empty list when no files found under prefix")
        void shouldReturnEmptyWhenNoFiles() {
            // given
            when(repository.findFilesByPathPrefix(USER_ID, "empty/"))
                    .thenReturn(List.of());
            when(mapper.toDto(List.of()))
                    .thenReturn(List.of());

            // when
            List<ResourceMetadataDto> result = service.findFilesByPathPrefix(USER_ID, "empty/");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindByNameContaining {

        @Test
        @DisplayName("Should delegate to repository with limit and return mapped results")
        void shouldDelegateAndReturnResults() {
            // given
            ResourceMetadata mockEntity = mock(ResourceMetadata.class);
            ResourceMetadataDto dto = new ResourceMetadataDto(
                    1L, USER_ID, "storage-key", "Docs/Report.txt", "docs/", "Report.txt",
                    200L, ResourceType.FILE);

            when(repository.findByNameContaining(eq(USER_ID), eq("report"), any()))
                    .thenReturn(List.of(mockEntity));
            when(mapper.toDto(List.of(mockEntity)))
                    .thenReturn(List.of(dto));

            // when
            List<ResourceMetadataDto> result = service.findByNameContaining(USER_ID, "report", 10);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().name()).isEqualTo("Report.txt");
        }

        @Test
        @DisplayName("Should return empty list when no name matches")
        void shouldReturnEmptyWhenNoMatch() {
            // given
            when(repository.findByNameContaining(eq(USER_ID), eq("xyz"), any()))
                    .thenReturn(List.of());
            when(mapper.toDto(List.of()))
                    .thenReturn(List.of());

            // when
            List<ResourceMetadataDto> result = service.findByNameContaining(USER_ID, "xyz", 10);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindFilesMarkedForDeletion {

        @Test
        @DisplayName("Should delegate to repository with limit and return mapped results")
        void shouldDelegateAndReturnResults() {
            // given
            ResourceMetadata mockEntity = mock(ResourceMetadata.class);
            ResourceMetadataDto dto = new ResourceMetadataDto(
                    1L, USER_ID, "storage-key", "Docs/File.txt", "docs/", "File.txt",
                    100L, ResourceType.FILE);

            when(repository.findFilesMarkedForDeletion(any()))
                    .thenReturn(List.of(mockEntity));
            when(mapper.toDto(List.of(mockEntity)))
                    .thenReturn(List.of(dto));

            // when
            List<ResourceMetadataDto> result = service.findFilesMarkedForDeletion(10);

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should return empty list when no files are marked for deletion")
        void shouldReturnEmptyWhenNoneMarked() {
            // given
            when(repository.findFilesMarkedForDeletion(any()))
                    .thenReturn(List.of());
            when(mapper.toDto(List.of()))
                    .thenReturn(List.of());

            // when
            List<ResourceMetadataDto> result = service.findFilesMarkedForDeletion(10);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class SaveFiles {

        @Test
        @DisplayName("Should create entities via factory and save all")
        void shouldCreateEntitiesAndSaveAll() {
            // given
            CreateFileDto fileDto = new CreateFileDto("key1", "Docs/File.txt", 100L);
            ResourceMetadata mockEntity = mock(ResourceMetadata.class);

            when(factory.createFiles(USER_ID, List.of(fileDto)))
                    .thenReturn(List.of(mockEntity));

            // when
            service.saveFiles(USER_ID, List.of(fileDto));

            // then
            verify(repository).saveAllAndFlush(List.of(mockEntity));
        }
    }

    @Nested
    class SaveDirectories {

        @Test
        @DisplayName("Should save only directories that do not already exist")
        void shouldSaveOnlyMissingDirectories() {
            // given
            Set<String> paths = Set.of("docs/");
            ResourceMetadata mockEntity = mock(ResourceMetadata.class);
            ResourceMetadataDto dto = new ResourceMetadataDto(
                    1L, USER_ID, null, "docs/", "", "docs",
                    null, ResourceType.DIRECTORY);

            when(repository.findExistingNormalizedPaths(USER_ID, paths))
                    .thenReturn(Set.of());
            when(factory.createDirectories(USER_ID, paths))
                    .thenReturn(List.of(mockEntity));
            when(mapper.toDto(List.of(mockEntity)))
                    .thenReturn(List.of(dto));

            // when
            List<ResourceMetadataDto> result = service.saveDirectories(USER_ID, paths);

            // then
            assertThat(result).hasSize(1);
            verify(repository).saveAllAndFlush(List.of(mockEntity));
        }

        @Test
        @DisplayName("Should return empty list and skip save when all directories already exist")
        void shouldReturnEmptyWhenAllExist() {
            // given
            Set<String> paths = Set.of("docs/");

            when(repository.findExistingNormalizedPaths(USER_ID, paths))
                    .thenReturn(Set.of("docs/"));

            // when
            List<ResourceMetadataDto> result = service.saveDirectories(USER_ID, paths);

            // then
            assertThat(result).isEmpty();
            verify(repository, never()).saveAllAndFlush(any());
        }
    }

    @Nested
    class SaveDirectory {

        @Test
        @DisplayName("Should map to entity and save")
        void shouldMapAndSave() {
            // given
            ResourceMetadata mockEntity = mock(ResourceMetadata.class);
            when(factory.createDirectory(eq(USER_ID), anyString()))
                    .thenReturn(mockEntity);

            // when
            service.saveDirectory(USER_ID, "Docs/");

            // then
            verify(repository).saveAndFlush(mockEntity);
        }

        @Test
        @DisplayName("Should throw ResourceAlreadyExistsException on duplicate")
        void shouldThrowOnDuplicate() {
            // given
            ResourceMetadata mockEntity = mock(ResourceMetadata.class);
            when(factory.createDirectory(USER_ID, "Docs/")).thenReturn(mockEntity);
            when(repository.saveAndFlush(mockEntity))
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
            service.deleteFileMetadata(USER_ID, "Docs/File.txt");

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
    class DeleteByIds {

        @Test
        @DisplayName("Should delegate ids to repository without transformation")
        void shouldDelegateToRepository() {
            // when
            service.deleteByIds(List.of(1L, 2L, 3L));

            // then
            verify(repository).deleteByIds(List.of(1L, 2L, 3L));
        }
    }

    @Nested
    class MarkDirectoryForDeletionAndCollectKeys {

        @Test
        @DisplayName("Should normalize path, collect file keys, mark directory, and return total size")
        void shouldNormalizeAndReturnResult() {
            // given
            ResourceMetadata file1 = mock(ResourceMetadata.class);
            ResourceMetadata file2 = mock(ResourceMetadata.class);
            when(file1.getSize()).thenReturn(100L);
            when(file1.getStorageKey()).thenReturn("key1");
            when(file2.getSize()).thenReturn(200L);
            when(file2.getStorageKey()).thenReturn("key2");

            when(repository.findFilesByPathPrefix(USER_ID, "docs/"))
                    .thenReturn(List.of(file1, file2));

            // when
            DeleteDirectoryResult result = service.markDirectoryForDeletionAndCollectKeys(USER_ID, "Docs/");

            // then
            assertThat(result.totalSize()).isEqualTo(300L);
            assertThat(result.deletedStorageKeys()).containsExactlyInAnyOrder("key1", "key2");
            verify(repository).findFilesByPathPrefix(USER_ID, "docs/");
            verify(repository).markForDeletionByNormalizedPath(USER_ID, "docs/");
        }

        @Test
        @DisplayName("Should return zero size and empty keys when directory has no files")
        void shouldReturnEmptyWhenNoFiles() {
            // given
            when(repository.findFilesByPathPrefix(USER_ID, "empty/"))
                    .thenReturn(List.of());

            // when
            DeleteDirectoryResult result = service.markDirectoryForDeletionAndCollectKeys(USER_ID, "empty/");

            // then
            assertThat(result.totalSize()).isEqualTo(0L);
            assertThat(result.deletedStorageKeys()).isEmpty();
            verify(repository).markForDeletionByNormalizedPath(USER_ID, "empty/");
        }
    }

    @Nested
    class ThrowIfAnyExists {

        @Test
        @DisplayName("Should not throw when no paths exist")
        void shouldNotThrowWhenNoneExist() {
            // given
            when(repository.findExistingPaths(USER_ID, Set.of("docs/file.txt")))
                    .thenReturn(Set.of());

            // when & then
            service.throwIfAnyExists(USER_ID, List.of("Docs/File.txt"));
        }

        @Test
        @DisplayName("Should throw when some paths already exist")
        void shouldThrowWhenSomeExist() {
            // given
            when(repository.findExistingPaths(USER_ID, Set.of("docs/file.txt", "work/report.txt")))
                    .thenReturn(Set.of("docs/file.txt"));

            // when & then
            assertThatThrownBy(() -> service.throwIfAnyExists(USER_ID,
                    List.of("Docs/File.txt", "Work/Report.txt")))
                    .isInstanceOf(ResourceAlreadyExistsException.class);
        }
    }

    @Nested
    class ThrowIfAnyConflictingTypeExists {

        @Test
        @DisplayName("Should not throw when no conflicting types exist")
        void shouldNotThrowWhenNoConflicts() {
            // given
            when(repository.findExistingPaths(USER_ID, Set.of("docs/")))
                    .thenReturn(Set.of());

            // when & then
            service.throwIfAnyConflictingTypeExists(USER_ID, List.of("docs"));
        }

        @Test
        @DisplayName("Should throw when file exists and directory is being created")
        void shouldThrowWhenFileConflictsWithDirectory() {
            // given
            when(repository.findExistingPaths(USER_ID, Set.of("docs")))
                    .thenReturn(Set.of("docs"));

            // when & then
            assertThatThrownBy(() -> service.throwIfAnyConflictingTypeExists(USER_ID,
                    List.of("docs/")))
                    .isInstanceOf(ResourceAlreadyExistsException.class);
        }

        @Test
        @DisplayName("Should throw when directory exists and file is being uploaded")
        void shouldThrowWhenDirectoryConflictsWithFile() {
            // given
            when(repository.findExistingPaths(USER_ID, Set.of("report/")))
                    .thenReturn(Set.of("report/"));

            // when & then
            assertThatThrownBy(() -> service.throwIfAnyConflictingTypeExists(USER_ID,
                    List.of("report")))
                    .isInstanceOf(ResourceAlreadyExistsException.class);
        }

        @Test
        @DisplayName("Should normalize opposite type paths before checking")
        void shouldNormalizeOppositeTypePaths() {
            // given
            when(repository.findExistingPaths(USER_ID, Set.of("docs/report")))
                    .thenReturn(Set.of());

            // when
            service.throwIfAnyConflictingTypeExists(USER_ID, List.of("Docs/Report/"));

            // then
            verify(repository).findExistingPaths(USER_ID, Set.of("docs/report"));
        }
    }
}