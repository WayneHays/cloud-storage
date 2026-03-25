package com.waynehays.cloudfilestorage.unit.service.metadata;

import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.repository.ResourceMetadataRepository;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataService;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceMetadataServiceTest {

    @Mock
    private ResourceMetadataRepository repository;

    @InjectMocks
    private ResourceMetadataService service;

    private static final Long USER_ID = 1L;

    @Nested
    class FindOrThrow {

        @Test
        void shouldReturnMetadataWhenFound() {
            // given
            String path = "directory/file.txt";
            ResourceMetadata metadata = createFileMetadata(path);

            when(repository.findByUserIdAndPathAndMarkedForDeletionFalse(USER_ID, path))
                    .thenReturn(Optional.of(metadata));

            // when
            ResourceMetadata result = service.findOrThrow(USER_ID, path);

            // then
            assertThat(result).isEqualTo(metadata);
        }

        @Test
        void shouldThrowWhenNotFound() {
            // given
            String path = "directory/file.txt";

            when(repository.findByUserIdAndPathAndMarkedForDeletionFalse(USER_ID, path))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.findOrThrow(USER_ID, path))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(path);
        }
    }

    @Nested
    class Exists {

        @Test
        void shouldReturnTrueWhenExists() {
            // given
            String path = "directory/file.txt";

            when(repository.existsByUserIdAndPathAndMarkedForDeletionFalse(USER_ID, path))
                    .thenReturn(true);

            // when & then
            assertThat(service.exists(USER_ID, path)).isTrue();
        }

        @Test
        void shouldReturnFalseWhenNotExists() {
            // given
            String path = "directory/file.txt";

            when(repository.existsByUserIdAndPathAndMarkedForDeletionFalse(USER_ID, path))
                    .thenReturn(false);

            // when & then
            assertThat(service.exists(USER_ID, path)).isFalse();
        }
    }

    @Nested
    class ThrowIfExists {

        @Test
        void shouldThrowWhenResourceExists() {
            // given
            String path = "directory/file.txt";

            when(repository.existsByUserIdAndPathAndMarkedForDeletionFalse(USER_ID, path))
                    .thenReturn(true);

            // when & then
            assertThatThrownBy(() -> service.throwIfExists(USER_ID, path))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining(path);
        }

        @Test
        void shouldNotThrowWhenResourceNotExists() {
            // given
            String path = "directory/file.txt";

            when(repository.existsByUserIdAndPathAndMarkedForDeletionFalse(USER_ID, path))
                    .thenReturn(false);

            // when & then
            assertThatCode(() -> service.throwIfExists(USER_ID, path))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class ThrowIfAnyExists {

        @Test
        void shouldThrowWhenAnyPathExists() {
            // given
            List<String> paths = List.of("directory/file1.txt", "directory/file2.txt");

            when(repository.existsByUserIdAndPathAndMarkedForDeletionFalse(USER_ID, "directory/file1.txt"))
                    .thenReturn(false);
            when(repository.existsByUserIdAndPathAndMarkedForDeletionFalse(USER_ID, "directory/file2.txt"))
                    .thenReturn(true);

            // when & then
            assertThatThrownBy(() -> service.throwIfAnyExists(USER_ID, paths))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining("file2.txt");
        }

        @Test
        void shouldNotThrowWhenNoneExist() {
            // given
            List<String> paths = List.of("directory/file1.txt", "directory/file2.txt");

            when(repository.existsByUserIdAndPathAndMarkedForDeletionFalse(eq(USER_ID), any()))
                    .thenReturn(false);

            // when & then
            assertThatCode(() -> service.throwIfAnyExists(USER_ID, paths))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class ValidateParentExists {

        @Test
        void shouldThrowWhenParentNotFound() {
            // given
            String path = "directory/subdirectory/";

            when(repository.findByUserIdAndPathAndMarkedForDeletionFalse(USER_ID, "directory/"))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.ensureParentExists(USER_ID, path))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("directory/");
        }

        @Test
        void shouldNotCheckParentForRootLevel() {
            // given
            String path = "directory/";

            // when
            service.ensureParentExists(USER_ID, path);

            // then
            verify(repository, never()).findByUserIdAndPathAndMarkedForDeletionFalse(any(), any());
        }

        @Test
        void shouldPassWhenParentExists() {
            // given
            String path = "directory/subdirectory/";
            ResourceMetadata parentMetadata = createDirectoryMetadata("directory/");

            when(repository.findByUserIdAndPathAndMarkedForDeletionFalse(USER_ID, "directory/"))
                    .thenReturn(Optional.of(parentMetadata));

            // when & then
            assertThatCode(() -> service.ensureParentExists(USER_ID, path))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class FindDirectChildren {

        @Test
        void shouldReturnDirectChildren() {
            // given
            String directoryPath = "directory/";
            ResourceMetadata dirMetadata = createDirectoryMetadata(directoryPath);
            ResourceMetadata child1 = createFileMetadata("directory/file.txt");
            ResourceMetadata child2 = createDirectoryMetadata("directory/sub/");

            when(repository.findByUserIdAndPathAndMarkedForDeletionFalse(USER_ID, directoryPath))
                    .thenReturn(Optional.of(dirMetadata));
            when(repository.findByUserIdAndParentPathAndMarkedForDeletionFalse(USER_ID, directoryPath))
                    .thenReturn(List.of(child1, child2));

            // when
            List<ResourceMetadata> result = service.findDirectChildren(USER_ID, directoryPath);

            // then
            assertThat(result).containsExactly(child1, child2);
        }

        @Test
        void shouldReturnEmptyListForEmptyDirectory() {
            // given
            String directoryPath = "empty/";
            ResourceMetadata dirMetadata = createDirectoryMetadata(directoryPath);

            when(repository.findByUserIdAndPathAndMarkedForDeletionFalse(USER_ID, directoryPath))
                    .thenReturn(Optional.of(dirMetadata));
            when(repository.findByUserIdAndParentPathAndMarkedForDeletionFalse(USER_ID, directoryPath))
                    .thenReturn(List.of());

            // when
            List<ResourceMetadata> result = service.findDirectChildren(USER_ID, directoryPath);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldThrowWhenDirectoryNotFound() {
            // given
            String directoryPath = "nonexistent/";

            when(repository.findByUserIdAndPathAndMarkedForDeletionFalse(USER_ID, directoryPath))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.findDirectChildren(USER_ID, directoryPath))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(directoryPath);
        }

        @Test
        void shouldSkipExistenceCheckForRoot() {
            // given
            String rootPath = "";
            ResourceMetadata child = createFileMetadata("file.txt");

            when(repository.findByUserIdAndParentPathAndMarkedForDeletionFalse(USER_ID, rootPath))
                    .thenReturn(List.of(child));

            // when
            List<ResourceMetadata> result = service.findDirectChildren(USER_ID, rootPath);

            // then
            assertThat(result).containsExactly(child);
            verify(repository, never()).findByUserIdAndPathAndMarkedForDeletionFalse(any(), any());
        }
    }

    @Nested
    class FindDirectoryContent {

        @Test
        void shouldReturnAllContentRecursively() {
            // given
            String prefix = "directory/";
            ResourceMetadata file1 = createFileMetadata("directory/file.txt");
            ResourceMetadata file2 = createFileMetadata("directory/sub/file2.txt");

            when(repository.findByUserIdAndPathStartingWithAndMarkedForDeletionFalse(USER_ID, prefix))
                    .thenReturn(List.of(file1, file2));

            // when
            List<ResourceMetadata> result = service.findDirectoryContent(USER_ID, prefix);

            // then
            assertThat(result).containsExactly(file1, file2);
        }
    }

    @Nested
    class FindByNameContaining {

        @Test
        void shouldReturnMatchingResources() {
            // given
            String query = "report";
            ResourceMetadata match = createFileMetadata("directory/report.pdf");

            when(repository.findByUserIdAndNameContainingIgnoreCaseAndMarkedForDeletionFalse(USER_ID, query))
                    .thenReturn(List.of(match));

            // when
            List<ResourceMetadata> result = service.findByNameContaining(USER_ID, query);

            // then
            assertThat(result).containsExactly(match);
        }
    }

    @Nested
    class FindMarkedForDeletion {

        @Test
        void shouldReturnMarkedRecords() {
            // given
            ResourceMetadata orphan = createFileMetadata("directory/file.txt");
            orphan.setMarkedForDeletion(true);

            when(repository.findByMarkedForDeletionTrue()).thenReturn(List.of(orphan));

            // when
            List<ResourceMetadata> result = service.findMarkedForDeletion();

            // then
            assertThat(result).containsExactly(orphan);
        }
    }

    @Nested
    class SaveFile {

        @Test
        void shouldSaveFileMetadata() {
            // given
            String path = "directory/file.txt";
            long size = 100L;

            // when
            service.saveFile(USER_ID, path, size);

            // then
            ArgumentCaptor<ResourceMetadata> captor = ArgumentCaptor.forClass(ResourceMetadata.class);
            verify(repository).save(captor.capture());

            ResourceMetadata saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getPath()).isEqualTo(path);
            assertThat(saved.getParentPath()).isEqualTo("directory/");
            assertThat(saved.getName()).isEqualTo("file.txt");
            assertThat(saved.getSize()).isEqualTo(size);
            assertThat(saved.getType()).isEqualTo(ResourceType.FILE);
            assertThat(saved.isMarkedForDeletion()).isFalse();
            assertThat(saved.getCreatedAt()).isNotNull();
        }
    }

    @Nested
    class SaveDirectory {

        @Test
        void shouldSaveDirectoryMetadata() {
            // given
            String path = "directory/subdirectory/";

            // when
            service.saveDirectory(USER_ID, path);

            // then
            ArgumentCaptor<ResourceMetadata> captor = ArgumentCaptor.forClass(ResourceMetadata.class);
            verify(repository).save(captor.capture());

            ResourceMetadata saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getPath()).isEqualTo(path);
            assertThat(saved.getParentPath()).isEqualTo("directory/");
            assertThat(saved.getName()).isEqualTo("subdirectory");
            assertThat(saved.getSize()).isNull();
            assertThat(saved.getType()).isEqualTo(ResourceType.DIRECTORY);
            assertThat(saved.isMarkedForDeletion()).isFalse();
        }
    }

    @Nested
    class MarkForDeletion {

        @Test
        void shouldMarkByExactPath() {
            // given
            String path = "directory/file.txt";

            // when
            service.markForDeletion(USER_ID, path);

            // then
            verify(repository).markForDeletion(USER_ID, path);
        }

        @Test
        void shouldMarkByPrefix() {
            // given
            String prefix = "directory/";

            // when
            service.markForDeletionByPrefix(USER_ID, prefix);

            // then
            verify(repository).markForDeletionByPrefix(USER_ID, prefix);
        }
    }

    @Nested
    class UpdatePath {

        @Test
        void shouldUpdatePathNameAndParentPath() {
            // given
            String pathFrom = "directory/old.txt";
            String pathTo = "other/new.txt";
            ResourceMetadata metadata = createFileMetadata(pathFrom);

            when(repository.findByUserIdAndPathAndMarkedForDeletionFalse(USER_ID, pathFrom))
                    .thenReturn(Optional.of(metadata));

            // when
            service.updatePath(USER_ID, pathFrom, pathTo);

            // then
            assertThat(metadata.getPath()).isEqualTo("other/new.txt");
            assertThat(metadata.getName()).isEqualTo("new.txt");
            assertThat(metadata.getParentPath()).isEqualTo("other/");
            assertThat(metadata.isMarkedForDeletion()).isFalse();
            verify(repository).save(metadata);
        }

        @Test
        void shouldThrowWhenSourceNotFound() {
            // given
            String pathFrom = "directory/file.txt";
            String pathTo = "other/file.txt";

            when(repository.findByUserIdAndPathAndMarkedForDeletionFalse(USER_ID, pathFrom))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.updatePath(USER_ID, pathFrom, pathTo))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(pathFrom);
        }
    }

    @Nested
    class UpdateContentPaths {

        @Test
        void shouldUpdateAllPathsAndUnmark() {
            // given
            ResourceMetadata file = createFileMetadata("old/file.txt");
            file.setMarkedForDeletion(true);
            ResourceMetadata subDir = createDirectoryMetadata("old/sub/");
            subDir.setMarkedForDeletion(true);
            List<ResourceMetadata> content = List.of(file, subDir);

            // when
            service.batchUpdatePaths(content, "old/", "new/");

            // then
            assertThat(file.getPath()).isEqualTo("new/file.txt");
            assertThat(file.getParentPath()).isEqualTo("new/");
            assertThat(file.getName()).isEqualTo("file.txt");
            assertThat(file.isMarkedForDeletion()).isFalse();

            assertThat(subDir.getPath()).isEqualTo("new/sub/");
            assertThat(subDir.getParentPath()).isEqualTo("new/");
            assertThat(subDir.getName()).isEqualTo("sub");
            assertThat(subDir.isMarkedForDeletion()).isFalse();

            verify(repository).saveAll(content);
        }

        @Test
        void shouldHandleNestedPaths() {
            // given
            ResourceMetadata file = createFileMetadata("docs/work/reports/file.txt");
            file.setMarkedForDeletion(true);
            List<ResourceMetadata> content = List.of(file);

            // when
            service.batchUpdatePaths(content, "docs/work/", "archive/work/");

            // then
            assertThat(file.getPath()).isEqualTo("archive/work/reports/file.txt");
            assertThat(file.getParentPath()).isEqualTo("archive/work/reports/");
            assertThat(file.getName()).isEqualTo("file.txt");
            assertThat(file.isMarkedForDeletion()).isFalse();

            verify(repository).saveAll(content);
        }
    }

    @Nested
    class Delete {

        @Test
        void shouldDeleteByPath() {
            // given
            String path = "directory/file.txt";

            // when
            service.delete(USER_ID, path);

            // then
            verify(repository).deleteByUserIdAndPath(USER_ID, path);
        }

        @Test
        void shouldDeleteByPrefix() {
            // given
            String prefix = "directory/";

            // when
            service.deleteByPrefix(USER_ID, prefix);

            // then
            verify(repository).deleteByUserIdAndPathStartingWith(USER_ID, prefix);
        }

        @Test
        void shouldDeleteById() {
            // given
            Long id = 42L;

            // when
            service.deleteById(id);

            // then
            verify(repository).deleteById(id);
        }
    }

    private ResourceMetadata createFileMetadata(String path) {
        ResourceMetadata metadata = new ResourceMetadata();
        metadata.setUserId(USER_ID);
        metadata.setPath(path);
        metadata.setParentPath(PathUtils.extractParentPath(path));
        metadata.setName(PathUtils.extractFilename(path));
        metadata.setSize(100L);
        metadata.setType(ResourceType.FILE);
        metadata.setMarkedForDeletion(false);
        metadata.setCreatedAt(Instant.now());
        return metadata;
    }

    private ResourceMetadata createDirectoryMetadata(String path) {
        ResourceMetadata metadata = new ResourceMetadata();
        metadata.setUserId(USER_ID);
        metadata.setPath(path);
        metadata.setParentPath(PathUtils.extractParentPath(path));
        metadata.setName(PathUtils.extractFilename(path));
        metadata.setType(ResourceType.DIRECTORY);
        metadata.setMarkedForDeletion(false);
        metadata.setCreatedAt(Instant.now());
        return metadata;
    }
}
