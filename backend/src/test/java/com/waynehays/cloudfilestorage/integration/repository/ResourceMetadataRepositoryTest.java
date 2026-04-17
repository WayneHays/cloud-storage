package com.waynehays.cloudfilestorage.integration.repository;

import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.repository.metadata.ResourceMetadataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResourceMetadataRepository integration tests")
class ResourceMetadataRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private ResourceMetadataRepository repository;

    @Nested
    @DisplayName("existsByNormalizedPath")
    class ExistsByNormalizedPath {

        @Test
        @DisplayName("Should return true when resource exists")
        void shouldReturnTrueWhenExists() {
            // given
            em.persist(file(userId, "docs/file.txt", "docs/", "file.txt", 10));
            em.flush();

            // when & then
            assertThat(repository.existsByNormalizedPath(userId, "docs/file.txt")).isTrue();
        }

        @Test
        @DisplayName("Should return false when resource does not exist")
        void shouldReturnFalseWhenNotExists() {
            // when & then
            assertThat(repository.existsByNormalizedPath(userId, "nonexistent.txt")).isFalse();
        }

        @Test
        @DisplayName("Should return false for other user's resource")
        void shouldReturnFalseForOtherUser() {
            // given
            em.persist(file(otherUserId, "docs/file.txt", "docs/", "file.txt", 10));
            em.flush();

            // when & then
            assertThat(repository.existsByNormalizedPath(userId, "docs/file.txt")).isFalse();
        }

        @Test
        @DisplayName("Should find resource regardless of original path case")
        void shouldFindCaseInsensitive() {
            // given
            em.persist(file(userId, "Docs/File.txt", "docs/", "File.txt", 10));
            em.flush();

            // when & then
            assertThat(repository.existsByNormalizedPath(userId, "docs/file.txt")).isTrue();
        }
    }

    @Nested
    @DisplayName("findByNormalizedPath")
    class FindByNormalizedPath {

        @Test
        @DisplayName("Should return resource when found")
        void shouldReturnWhenFound() {
            // given
            em.persist(file(userId, "Docs/File.txt", "docs/", "File.txt", 10));
            em.flush();

            // when
            Optional<ResourceMetadata> result = repository.findByNormalizedPath(userId, "docs/file.txt");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getPath()).isEqualTo("Docs/File.txt");
            assertThat(result.get().getName()).isEqualTo("File.txt");
        }

        @Test
        @DisplayName("Should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            // when & then
            assertThat(repository.findByNormalizedPath(userId, "missing.txt")).isEmpty();
        }

        @Test
        @DisplayName("Should exclude resources marked for deletion")
        void shouldExcludeMarkedForDeletion() {
            // given
            ResourceMetadata deleted = file(userId, "docs/file.txt", "docs/", "file.txt", 10);
            deleted.setMarkedForDeletion(true);
            em.persist(deleted);
            em.flush();

            // when & then
            assertThat(repository.findByNormalizedPath(userId, "docs/file.txt")).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByParentPath")
    class FindByParentPath {

        @Test
        @DisplayName("Should return direct children of directory")
        void shouldReturnDirectChildren() {
            // given
            em.persist(directory(userId, "docs/", "", "docs"));
            em.persist(file(userId, "docs/a.txt", "docs/", "a.txt", 10));
            em.persist(file(userId, "docs/b.txt", "docs/", "b.txt", 20));
            em.persist(directory(userId, "docs/sub/", "docs/", "sub"));
            em.persist(file(userId, "docs/sub/deep.txt", "docs/sub/", "deep.txt", 30));
            em.flush();

            // when
            List<ResourceMetadata> result = repository.findByParentPath(userId, "docs/");

            // then
            assertThat(result).hasSize(3);
            assertThat(result).extracting(ResourceMetadata::getName)
                    .containsExactlyInAnyOrder("a.txt", "b.txt", "sub");
        }

        @Test
        @DisplayName("Should return root-level resources for empty parent path")
        void shouldReturnRootResources() {
            // given
            em.persist(directory(userId, "docs/", "", "docs"));
            em.persist(file(userId, "root.txt", "", "root.txt", 10));
            em.flush();

            // when
            List<ResourceMetadata> result = repository.findByParentPath(userId, "");

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should exclude resources marked for deletion")
        void shouldExcludeMarkedForDeletion() {
            // given
            em.persist(directory(userId, "docs/", "", "docs"));
            ResourceMetadata deleted = file(userId, "docs/deleted.txt", "docs/", "deleted.txt", 10);
            deleted.setMarkedForDeletion(true);
            em.persist(deleted);
            em.persist(file(userId, "docs/active.txt", "docs/", "active.txt", 20));
            em.flush();

            // when
            List<ResourceMetadata> result = repository.findByParentPath(userId, "docs/");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getName()).isEqualTo("active.txt");
        }
    }

    @Nested
    @DisplayName("findExistingPaths")
    class FindExistingPaths {

        @Test
        @DisplayName("Should return paths that exist in database")
        void shouldReturnExistingPaths() {
            // given
            em.persist(file(userId, "Docs/A.txt", "docs/", "A.txt", 10));
            em.persist(file(userId, "Docs/B.txt", "docs/", "B.txt", 20));
            em.flush();

            // when
            Set<String> result = repository.findExistingPaths(userId,
                    Set.of("docs/a.txt", "docs/b.txt", "docs/c.txt"));

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should return original path case, not normalized")
        void shouldReturnOriginalCase() {
            // given
            em.persist(file(userId, "Docs/MyFile.txt", "docs/", "MyFile.txt", 10));
            em.flush();

            // when
            Set<String> result = repository.findExistingPaths(userId, Set.of("docs/myfile.txt"));

            // then
            assertThat(result).containsExactly("Docs/MyFile.txt");
        }

        @Test
        @DisplayName("Should exclude resources marked for deletion")
        void shouldExcludeMarkedForDeletion() {
            // given
            ResourceMetadata deleted = file(userId, "docs/deleted.txt", "docs/", "deleted.txt", 10);
            deleted.setMarkedForDeletion(true);
            em.persist(deleted);
            em.flush();

            // when
            Set<String> result = repository.findExistingPaths(userId, Set.of("docs/deleted.txt"));

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteFileByNormalizedPath")
    class DeleteFileByNormalizedPath {

        @Test
        @DisplayName("Should delete existing file")
        void shouldDeleteExistingFile() {
            // given
            em.persist(file(userId, "Docs/File.txt", "docs/", "File.txt", 10));
            em.flush();

            // when
            repository.deleteFileByNormalizedPath(userId, "docs/file.txt");

            // then
            assertThat(repository.findByNormalizedPath(userId, "docs/file.txt")).isEmpty();
        }

        @Test
        @DisplayName("Should not delete directory with same path")
        void shouldNotDeleteDirectory() {
            // given
            em.persist(directory(userId, "docs/", "", "docs"));
            em.flush();

            // when
            repository.deleteFileByNormalizedPath(userId, "docs/");

            // then
            assertThat(repository.existsByNormalizedPath(userId, "docs/")).isTrue();
        }

        @Test
        @DisplayName("Should not delete other user's file")
        void shouldNotDeleteOtherUsersFile() {
            // given
            em.persist(file(userId, "docs/file.txt", "docs/", "file.txt", 10));
            em.persist(file(otherUserId, "docs/file.txt", "docs/", "file.txt", 10));
            em.flush();

            // when
            repository.deleteFileByNormalizedPath(userId, "docs/file.txt");

            // then
            assertThat(repository.existsByNormalizedPath(otherUserId, "docs/file.txt")).isTrue();
        }
    }

    @Nested
    @DisplayName("deleteByNormalizedPathPrefix")
    class DeleteByNormalizedPathPrefix {

        @Test
        @DisplayName("Should delete directory and all nested resources")
        void shouldDeleteDirectoryAndContents() {
            // given
            em.persist(directory(userId, "docs/", "", "docs"));
            em.persist(file(userId, "docs/a.txt", "docs/", "a.txt", 10));
            em.persist(directory(userId, "docs/sub/", "docs/", "sub"));
            em.persist(file(userId, "docs/sub/b.txt", "docs/sub/", "b.txt", 20));
            em.flush();

            // when
            repository.deleteByNormalizedPathPrefix(userId, "docs/");

            // then
            assertThat(repository.existsByNormalizedPath(userId, "docs/")).isFalse();
            assertThat(repository.existsByNormalizedPath(userId, "docs/a.txt")).isFalse();
            assertThat(repository.existsByNormalizedPath(userId, "docs/sub/")).isFalse();
            assertThat(repository.existsByNormalizedPath(userId, "docs/sub/b.txt")).isFalse();
        }

        @Test
        @DisplayName("Should not delete resources with similar path prefix")
        void shouldNotDeleteSimilarPrefix() {
            // given
            em.persist(directory(userId, "docs/", "", "docs"));
            em.persist(directory(userId, "docs-backup/", "", "docs-backup"));
            em.flush();

            // when
            repository.deleteByNormalizedPathPrefix(userId, "docs/");

            // then
            assertThat(repository.existsByNormalizedPath(userId, "docs-backup/")).isTrue();
        }
    }

    @Nested
    @DisplayName("moveMetadata")
    class MoveMetadata {

        @Test
        @DisplayName("Should correctly move file to another directory")
        void shouldMoveFileToAnotherDirectory() {
            // given
            em.persist(directory(userId, "docs/", "", "docs"));
            em.persist(directory(userId, "work/", "", "work"));
            em.persist(file(userId, "docs/file.txt", "docs/", "file.txt", 10));
            em.flush();

            // when
            int moved = repository.moveMetadata(userId, "docs/file.txt",
                    "work/file.txt", "work/", "file.txt");

            // then
            assertThat(moved).isEqualTo(1);
            Optional<ResourceMetadata> result = repository.findByNormalizedPath(userId, "work/file.txt");
            assertThat(result).isPresent();
            assertThat(result.get().getPath()).isEqualTo("work/file.txt");
            assertThat(result.get().getParentPath()).isEqualTo("work/");
            assertThat(result.get().getNormalizedPath()).isEqualTo("work/file.txt");
        }

        @Test
        @DisplayName("Should correctly move file to root")
        void shouldMoveFileToRoot() {
            // given
            em.persist(directory(userId, "docs/", "", "docs"));
            em.persist(file(userId, "docs/file.txt", "docs/", "file.txt", 10));
            em.flush();

            // when
            int moved = repository.moveMetadata(userId, "docs/file.txt",
                    "file.txt", "", "file.txt");

            // then
            assertThat(moved).isEqualTo(1);
            Optional<ResourceMetadata> result = repository.findByNormalizedPath(userId, "file.txt");
            assertThat(result).isPresent();
            assertThat(result.get().getParentPath()).isEqualTo("");
        }

        @Test
        @DisplayName("Should correctly set parent_path when moving empty directory")
        void shouldSetParentPathForEmptyDirectory() {
            // given
            em.persist(directory(userId, "docs/", "", "docs"));
            em.persist(directory(userId, "archive/", "", "archive"));
            em.flush();

            // when
            int moved = repository.moveMetadata(userId, "docs/",
                    "archive/docs/", "archive/", "docs");

            // then
            assertThat(moved).isEqualTo(1);
            Optional<ResourceMetadata> result = repository.findByNormalizedPath(userId, "archive/docs/");
            assertThat(result).isPresent();
            assertThat(result.get().getParentPath()).isEqualTo("archive/");
        }

        @Test
        @DisplayName("Should correctly update paths for directory with nested files")
        void shouldMoveDirectoryWithNestedFiles() {
            // given
            em.persist(directory(userId, "docs/", "", "docs"));
            em.persist(directory(userId, "archive/", "", "archive"));
            em.persist(file(userId, "docs/a.txt", "docs/", "a.txt", 10));
            em.persist(file(userId, "docs/b.txt", "docs/", "b.txt", 20));
            em.flush();

            // when
            int moved = repository.moveMetadata(userId, "docs/",
                    "archive/docs/", "archive/", "docs");

            // then
            assertThat(moved).isEqualTo(3);

            Optional<ResourceMetadata> dir = repository.findByNormalizedPath(userId, "archive/docs/");
            assertThat(dir).isPresent();
            assertThat(dir.get().getParentPath()).isEqualTo("archive/");

            Optional<ResourceMetadata> fileA = repository.findByNormalizedPath(userId, "archive/docs/a.txt");
            assertThat(fileA).isPresent();
            assertThat(fileA.get().getParentPath()).isEqualTo("archive/docs/");

            Optional<ResourceMetadata> fileB = repository.findByNormalizedPath(userId, "archive/docs/b.txt");
            assertThat(fileB).isPresent();
            assertThat(fileB.get().getParentPath()).isEqualTo("archive/docs/");
        }

        @Test
        @DisplayName("Should correctly update paths for directory with nested subdirectory")
        void shouldMoveDirectoryWithNestedSubdirectory() {
            // given
            em.persist(directory(userId, "docs/", "", "docs"));
            em.persist(directory(userId, "docs/sub/", "docs/", "sub"));
            em.persist(file(userId, "docs/sub/deep.txt", "docs/sub/", "deep.txt", 10));
            em.persist(directory(userId, "archive/", "", "archive"));
            em.flush();

            // when
            int moved = repository.moveMetadata(userId, "docs/",
                    "archive/docs/", "archive/", "docs");

            // then
            assertThat(moved).isEqualTo(3);

            Optional<ResourceMetadata> sub = repository.findByNormalizedPath(userId, "archive/docs/sub/");
            assertThat(sub).isPresent();
            assertThat(sub.get().getParentPath()).isEqualTo("archive/docs/");

            Optional<ResourceMetadata> deep = repository.findByNormalizedPath(userId, "archive/docs/sub/deep.txt");
            assertThat(deep).isPresent();
            assertThat(deep.get().getParentPath()).isEqualTo("archive/docs/sub/");
        }

        @Test
        @DisplayName("Should update name when renaming resource")
        void shouldUpdateNameWhenRenaming() {
            // given
            em.persist(directory(userId, "docs/", "", "docs"));
            em.flush();

            // when
            repository.moveMetadata(userId, "docs/", "Reports/", "", "Reports");

            // then
            Optional<ResourceMetadata> result = repository.findByNormalizedPath(userId, "reports/");
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Reports");
            assertThat(result.get().getPath()).isEqualTo("Reports/");
        }

        @Test
        @DisplayName("Should correctly move directory to root")
        void shouldMoveDirectoryToRoot() {
            // given
            em.persist(directory(userId, "archive/", "", "archive"));
            em.persist(directory(userId, "archive/docs/", "archive/", "docs"));
            em.persist(file(userId, "archive/docs/file.txt", "archive/docs/", "file.txt", 10));
            em.flush();

            // when
            int moved = repository.moveMetadata(userId, "archive/docs/",
                    "docs/", "", "docs");

            // then
            assertThat(moved).isEqualTo(2);

            Optional<ResourceMetadata> dir = repository.findByNormalizedPath(userId, "docs/");
            assertThat(dir).isPresent();
            assertThat(dir.get().getParentPath()).isEqualTo("");

            Optional<ResourceMetadata> f = repository.findByNormalizedPath(userId, "docs/file.txt");
            assertThat(f).isPresent();
            assertThat(f.get().getParentPath()).isEqualTo("docs/");
        }

        @Test
        @DisplayName("Should return zero when moving non-existent resource")
        void shouldReturnZeroWhenNotFound() {
            // when
            int moved = repository.moveMetadata(userId, "nonexistent/",
                    "somewhere/", "", "somewhere");

            // then
            assertThat(moved).isEqualTo(0);
        }

        @Test
        @DisplayName("Should not move resources of other user")
        void shouldNotMoveOtherUsersResources() {
            // given
            em.persist(directory(userId, "docs/", "", "docs"));
            em.persist(directory(otherUserId, "docs/", "", "docs"));
            em.persist(file(otherUserId, "docs/file.txt", "docs/", "file.txt", 10));
            em.flush();

            // when
            repository.moveMetadata(userId, "docs/", "archive/docs/", "archive/", "docs");

            // then
            Optional<ResourceMetadata> otherFile = repository.findByNormalizedPath(otherUserId, "docs/file.txt");
            assertThat(otherFile).isPresent();
            assertThat(otherFile.get().getParentPath()).isEqualTo("docs/");
        }

        @Test
        @DisplayName("Should not move resources with similar path prefix")
        void shouldNotMoveSimilarPrefix() {
            // given
            em.persist(directory(userId, "docs/", "", "docs"));
            em.persist(directory(userId, "docs-backup/", "", "docs-backup"));
            em.persist(file(userId, "docs-backup/file.txt", "docs-backup/", "file.txt", 10));
            em.flush();

            // when
            repository.moveMetadata(userId, "docs/", "archive/docs/", "archive/", "docs");

            // then
            Optional<ResourceMetadata> backup = repository.findByNormalizedPath(userId, "docs-backup/file.txt");
            assertThat(backup).isPresent();
            assertThat(backup.get().getParentPath()).isEqualTo("docs-backup/");
        }

        @Test
        @DisplayName("Should update normalized_path for all moved resources")
        void shouldUpdateNormalizedPath() {
            // given
            em.persist(directory(userId, "docs/", "", "docs"));
            em.persist(file(userId, "docs/file.txt", "docs/", "file.txt", 10));
            em.flush();

            // when
            repository.moveMetadata(userId, "docs/", "Archive/Docs/", "archive/", "Docs");

            // then
            Optional<ResourceMetadata> dir = repository.findByNormalizedPath(userId, "archive/docs/");
            assertThat(dir).isPresent();
            assertThat(dir.get().getPath()).isEqualTo("Archive/Docs/");
            assertThat(dir.get().getNormalizedPath()).isEqualTo("archive/docs/");

            Optional<ResourceMetadata> f = repository.findByNormalizedPath(userId, "archive/docs/file.txt");
            assertThat(f).isPresent();
            assertThat(f.get().getPath()).isEqualTo("Archive/Docs/file.txt");
            assertThat(f.get().getNormalizedPath()).isEqualTo("archive/docs/file.txt");
        }
    }

    @Nested
    @DisplayName("markForDeletionByNormalizedPath")
    class MarkForDeletionByNormalizedPath {

        @Test
        @DisplayName("Should mark resource for deletion")
        void shouldMarkForDeletion() {
            // given
            em.persist(file(userId, "docs/file.txt", "docs/", "file.txt", 10));
            em.flush();

            // when
            repository.markForDeletionByNormalizedPath(userId, "docs/file.txt");

            // then
            assertThat(repository.findByNormalizedPath(userId, "docs/file.txt")).isEmpty();
            assertThat(repository.existsByNormalizedPath(userId, "docs/file.txt")).isTrue();
        }
    }

    // === deleteByNormalizedPaths ===

    @Nested
    @DisplayName("deleteByNormalizedPaths")
    class DeleteByNormalizedPaths {

        @Test
        @DisplayName("Should delete multiple resources by normalized paths")
        void shouldDeleteMultiplePaths() {
            // given
            em.persist(file(userId, "a.txt", "", "a.txt", 10));
            em.persist(file(userId, "b.txt", "", "b.txt", 20));
            em.persist(file(userId, "c.txt", "", "c.txt", 30));
            em.flush();

            // when
            repository.deleteByNormalizedPaths(userId, List.of("a.txt", "b.txt"));

            // then
            assertThat(repository.existsByNormalizedPath(userId, "a.txt")).isFalse();
            assertThat(repository.existsByNormalizedPath(userId, "b.txt")).isFalse();
            assertThat(repository.existsByNormalizedPath(userId, "c.txt")).isTrue();
        }
    }

    @Nested
    @DisplayName("deleteByIds")
    class DeleteByIds {

        @Test
        @DisplayName("Should delete resources by ids")
        void shouldDeleteByIds() {
            // given
            ResourceMetadata f1 = file(userId, "a.txt", "", "a.txt", 10);
            ResourceMetadata f2 = file(userId, "b.txt", "", "b.txt", 20);
            em.persist(f1);
            em.persist(f2);
            em.flush();

            // when
            repository.deleteByIds(List.of(f1.getId(), f2.getId()));

            // then
            assertThat(repository.existsByNormalizedPath(userId, "a.txt")).isFalse();
            assertThat(repository.existsByNormalizedPath(userId, "b.txt")).isFalse();
        }
    }
}

