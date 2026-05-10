package com.waynehays.cloudfilestorage.core.metadata.repository;

import com.waynehays.cloudfilestorage.AbstractRepositoryTest;
import com.waynehays.cloudfilestorage.core.metadata.entity.ResourceMetadata;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceMetadataRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private ResourceMetadataRepository repository;

    private TypedQuery<ResourceMetadata> executeQuery() {
        return em.getEntityManager()
                .createQuery("""
                        SELECT r
                        FROM ResourceMetadata r
                        WHERE r.userId = :userId
                        """, ResourceMetadata.class)
                .setParameter("userId", userId);
    }

    private List<ResourceMetadata> getSavedList() {
        return executeQuery().getResultList();
    }

    private ResourceMetadata getSavedSingle() {
        return executeQuery().getSingleResult();
    }

    @Nested
    class ExistsByNormalizedPath {

        @Test
        @DisplayName("Should return true when resource exists")
        void shouldReturnTrueWhenExists() {
            // given
            persistFile(userId, "key", "docs/file.txt", 10);

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
            persistFile(otherUserId, "key", "docs/file.txt", 10);

            // when & then
            assertThat(repository.existsByNormalizedPath(userId, "docs/file.txt")).isFalse();
        }

        @Test
        @DisplayName("Should find resource regardless of original path case")
        void shouldFindCaseInsensitive() {
            // given
            persistFile(userId, "key", "Docs/File.txt", 10);

            // when & then
            assertThat(repository.existsByNormalizedPath(userId, "docs/file.txt")).isTrue();
        }
    }

    @Nested
    class FindExistingNormalizedPaths {

        @Test
        @DisplayName("Should return normalized paths that exist in database")
        void shouldReturnExistingNormalizedPaths() {
            // given
            persistFile(userId, "key1", "Docs/A.txt", 10);
            persistFile(userId, "key2", "Docs/B.txt", 20);

            // when
            Set<String> result = repository.findExistingNormalizedPaths(userId,
                    Set.of("docs/a.txt", "docs/b.txt", "docs/c.txt"));

            // then
            assertThat(result).containsExactlyInAnyOrder("docs/a.txt", "docs/b.txt");
        }

        @Test
        @DisplayName("Should return empty set when no paths match")
        void shouldReturnEmptyWhenNoMatch() {
            // when
            Set<String> result = repository.findExistingNormalizedPaths(userId,
                    Set.of("nonexistent.txt"));

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should exclude resources marked for deletion")
        void shouldExcludeMarkedForDeletion() {
            // given
            ResourceMetadata deleted = file(userId, "key", "docs/file.txt", 10);
            deleted.setMarkedForDeletion(true);
            em.persist(deleted);
            em.flush();

            // when
            Set<String> result = repository.findExistingNormalizedPaths(userId,
                    Set.of("docs/file.txt"));

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should exclude other user's resources")
        void shouldExcludeOtherUsersResources() {
            // given
            persistFile(otherUserId, "key", "docs/file.txt", 10);

            // when
            Set<String> result = repository.findExistingNormalizedPaths(userId,
                    Set.of("docs/file.txt"));

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindByNormalizedPath {

        @Test
        @DisplayName("Should return resource when found")
        void shouldReturnWhenFound() {
            // given
            persistFile(userId, "key", "Docs/File.txt", 10);

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
            ResourceMetadata deleted = file(userId, "key", "docs/file.txt", 10);
            deleted.setMarkedForDeletion(true);
            em.persist(deleted);
            em.flush();

            // when & then
            assertThat(repository.findByNormalizedPath(userId, "docs/file.txt")).isEmpty();
        }
    }

    @Nested
    class FindByParentPath {

        @Test
        @DisplayName("Should return direct children of directory")
        void shouldReturnDirectChildren() {
            // given
            persistDirectory(userId, "docs/");
            persistFile(userId, "key1", "docs/a.txt", 10);
            persistFile(userId, "key2", "docs/b.txt", 20);
            persistDirectory(userId, "docs/sub/");
            persistFile(userId, "key3", "docs/sub/deep.txt", 30);

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
            persistDirectory(userId, "docs/");
            persistFile(userId, "key", "root.txt", 10);

            // when
            List<ResourceMetadata> result = repository.findByParentPath(userId, "");

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should exclude resources marked for deletion")
        void shouldExcludeMarkedForDeletion() {
            // given
            persistDirectory(userId, "docs/");
            ResourceMetadata deleted = file(userId, "key", "docs/deleted.txt", 10);
            deleted.setMarkedForDeletion(true);
            em.persist(deleted);
            persistFile(userId, "key1", "docs/active.txt", 20);

            // when
            List<ResourceMetadata> result = repository.findByParentPath(userId, "docs/");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getName()).isEqualTo("active.txt");
        }
    }

    @Nested
    class FindByNameContaining {

        @Test
        @DisplayName("Should return resources with matching name")
        void shouldReturnMatchingResources() {
            // given
            persistFile(userId, "key1", "docs/report.txt", 10);
            persistFile(userId, "key2", "docs/notes.txt", 20);

            // when
            List<ResourceMetadata> result = repository.findByNameContaining(userId, "report", Pageable.ofSize(10));

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getName()).isEqualTo("report.txt");
        }

        @Test
        @DisplayName("Should match partial name")
        void shouldMatchPartialName() {
            // given
            persistFile(userId, "key1", "docs/quarterly-report.txt", 10);
            persistFile(userId, "key2", "docs/annual-report.txt", 20);
            persistFile(userId, "key3", "docs/notes.txt", 30);

            // when
            List<ResourceMetadata> result = repository.findByNameContaining(userId, "report", Pageable.ofSize(10));

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(ResourceMetadata::getName)
                    .containsExactlyInAnyOrder("quarterly-report.txt", "annual-report.txt");
        }

        @Test
        @DisplayName("Should be case-insensitive")
        void shouldBeCaseInsensitive() {
            // given
            persistFile(userId, "key", "docs/Report.txt", 10);

            // when
            List<ResourceMetadata> result = repository.findByNameContaining(userId, "REPORT", Pageable.ofSize(10));

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should return empty list when no name matches")
        void shouldReturnEmptyWhenNoMatch() {
            // given
            persistFile(userId, "key", "docs/file.txt", 10);

            // when
            List<ResourceMetadata> result = repository.findByNameContaining(userId, "xyz", Pageable.ofSize(10));

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should exclude resources marked for deletion")
        void shouldExcludeMarkedForDeletion() {
            // given
            ResourceMetadata deleted = file(userId, "key", "docs/report.txt", 10);
            deleted.setMarkedForDeletion(true);
            em.persist(deleted);
            em.flush();

            // when
            List<ResourceMetadata> result = repository.findByNameContaining(userId, "report", Pageable.ofSize(10));

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should exclude other user's resources")
        void shouldExcludeOtherUsersResources() {
            // given
            persistFile(otherUserId, "key", "docs/report.txt", 10);

            // when
            List<ResourceMetadata> result = repository.findByNameContaining(userId, "report", Pageable.ofSize(10));

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should respect page size limit")
        void shouldRespectLimit() {
            // given
            persistFile(userId, "key1", "docs/report1.txt", 10);
            persistFile(userId, "key2", "docs/report2.txt", 20);
            persistFile(userId, "key3", "docs/report3.txt", 30);

            // when
            List<ResourceMetadata> result = repository.findByNameContaining(userId, "report", Pageable.ofSize(2));

            // then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    class FindFilesMarkedForDeletion {

        @Test
        @DisplayName("Should return files marked for deletion")
        void shouldReturnMarkedFiles() {
            // given
            ResourceMetadata deleted = file(userId, "key", "docs/file.txt", 10);
            deleted.setMarkedForDeletion(true);
            em.persist(deleted);
            em.flush();

            // when
            List<ResourceMetadata> result = repository.findFilesMarkedForDeletion(Pageable.ofSize(10));

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getStorageKey()).isEqualTo("key");
        }

        @Test
        @DisplayName("Should not return active files")
        void shouldNotReturnActiveFiles() {
            // given
            persistFile(userId, "key", "docs/file.txt", 10);

            // when
            List<ResourceMetadata> result = repository.findFilesMarkedForDeletion(Pageable.ofSize(10));

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should not return directories marked for deletion")
        void shouldNotReturnDirectories() {
            // given
            ResourceMetadata dir = directory(userId, "docs/");
            dir.setMarkedForDeletion(true);
            em.persist(dir);
            em.flush();

            // when
            List<ResourceMetadata> result = repository.findFilesMarkedForDeletion(Pageable.ofSize(10));

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return files from all users")
        void shouldReturnFilesFromAllUsers() {
            // given
            ResourceMetadata file1 = file(userId, "key1", "docs/a.txt", 10);
            file1.setMarkedForDeletion(true);
            ResourceMetadata file2 = file(otherUserId, "key2", "docs/b.txt", 20);
            file2.setMarkedForDeletion(true);
            em.persist(file1);
            em.persist(file2);
            em.flush();

            // when
            List<ResourceMetadata> result = repository.findFilesMarkedForDeletion(Pageable.ofSize(10));

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should respect page size limit")
        void shouldRespectLimit() {
            // given
            ResourceMetadata f1 = file(userId, "key1", "a.txt", 10);
            f1.setMarkedForDeletion(true);
            ResourceMetadata f2 = file(userId, "key2", "b.txt", 20);
            f2.setMarkedForDeletion(true);
            ResourceMetadata f3 = file(userId, "key3", "c.txt", 30);
            f3.setMarkedForDeletion(true);
            em.persist(f1);
            em.persist(f2);
            em.persist(f3);
            em.flush();

            // when
            List<ResourceMetadata> result = repository.findFilesMarkedForDeletion(Pageable.ofSize(2));

            // then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    class FindFilesByPathPrefix {

        @Test
        @DisplayName("Should return files matching normalized prefix")
        void shouldReturnFilesMatchingPrefix() {
            // given
            persistFile(userId, "key1", "docs/a.txt", 10);
            persistFile(userId, "key2", "docs/b.txt", 20);
            persistFile(userId, "key3", "work/c.txt", 30);

            // when
            List<ResourceMetadata> result = repository.findFilesByPathPrefix(userId, "docs/");

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(ResourceMetadata::getName)
                    .containsExactlyInAnyOrder("a.txt", "b.txt");
        }

        @Test
        @DisplayName("Should return files in nested subdirectories")
        void shouldReturnFilesInSubdirectories() {
            // given
            persistFile(userId, "key1", "docs/sub/a.txt", 10);
            persistFile(userId, "key2", "docs/sub/deep/b.txt", 20);

            // when
            List<ResourceMetadata> result = repository.findFilesByPathPrefix(userId, "docs/");

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should not return directories")
        void shouldNotReturnDirectories() {
            // given
            persistDirectory(userId, "docs/");
            persistFile(userId, "key", "docs/file.txt", 10);

            // when
            List<ResourceMetadata> result = repository.findFilesByPathPrefix(userId, "docs/");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getName()).isEqualTo("file.txt");
        }

        @Test
        @DisplayName("Should exclude files marked for deletion")
        void shouldExcludeMarkedForDeletion() {
            // given
            ResourceMetadata deleted = file(userId, "key", "docs/file.txt", 10);
            deleted.setMarkedForDeletion(true);
            em.persist(deleted);
            em.flush();

            // when
            List<ResourceMetadata> result = repository.findFilesByPathPrefix(userId, "docs/");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should exclude other user's files")
        void shouldExcludeOtherUsersFiles() {
            // given
            persistFile(otherUserId, "key", "docs/file.txt", 10);

            // when
            List<ResourceMetadata> result = repository.findFilesByPathPrefix(userId, "docs/");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should not return files with similar path prefix")
        void shouldNotReturnFilesWithSimilarPrefix() {
            // given
            persistFile(userId, "key1", "docs/file.txt", 10);
            persistFile(userId, "key2", "docs-backup/file.txt", 20);

            // when
            List<ResourceMetadata> result = repository.findFilesByPathPrefix(userId, "docs/");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getParentPath()).isEqualTo("docs/");
        }
    }

    @Nested
    class FindExistingPaths {

        @Test
        @DisplayName("Should return paths that exist in database")
        void shouldReturnExistingPaths() {
            // given
            persistFile(userId, "key1", "Docs/A.txt", 10);
            persistFile(userId, "key2", "Docs/B.txt", 20);

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
            String path = "Docs/MyFile.txt";
            persistFile(userId, "key", path, 10);

            // when
            Set<String> result = repository.findExistingPaths(userId, Set.of("docs/myfile.txt"));

            // then
            assertThat(result).containsExactly(path);
        }

        @Test
        @DisplayName("Should exclude resources marked for deletion")
        void shouldExcludeMarkedForDeletion() {
            // given
            ResourceMetadata deleted = file(userId, "key", "docs/deleted.txt", 10);
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
    class MarkForDeletionByNormalizedPath {

        @Test
        @DisplayName("Should mark resource for deletion")
        void shouldMarkForDeletion() {
            // given
            String path = "docs/file.txt";
            persistFile(userId, "key", path, 10);

            // when
            repository.markForDeletionByNormalizedPath(userId, path);

            // then
            assertThat(repository.findByNormalizedPath(userId, path)).isEmpty();
            assertThat(repository.existsByNormalizedPath(userId, path)).isFalse();
        }
    }

    @Nested
    class DeleteFileByNormalizedPath {

        @Test
        @DisplayName("Should delete existing file")
        void shouldDeleteExistingFile() {
            // given
            persistFile(userId, "key", "Docs/File.txt", 10);

            // when
            repository.deleteFileByNormalizedPath(userId, "docs/file.txt");

            // then
            assertThat(repository.findByNormalizedPath(userId, "docs/file.txt")).isEmpty();
        }

        @Test
        @DisplayName("Should not delete directory with same path")
        void shouldNotDeleteDirectory() {
            // given
            persistDirectory(userId, "docs/");

            // when
            repository.deleteFileByNormalizedPath(userId, "docs/");

            // then
            assertThat(repository.existsByNormalizedPath(userId, "docs/")).isTrue();
        }

        @Test
        @DisplayName("Should not delete other user's file")
        void shouldNotDeleteOtherUsersFile() {
            // given
            persistFile(userId, "key", "docs/file.txt", 10);
            persistFile(otherUserId, "key", "docs/file.txt", 10);

            // when
            repository.deleteFileByNormalizedPath(userId, "docs/file.txt");

            // then
            assertThat(repository.existsByNormalizedPath(otherUserId, "docs/file.txt")).isTrue();
        }
    }

    @Nested
    class DeleteByNormalizedPathPrefix {

        @Test
        @DisplayName("Should delete directory and all nested resources")
        void shouldDeleteDirectoryAndContents() {
            // given
            persistDirectory(userId, "docs/");
            persistFile(userId, "key1", "docs/a.txt", 10);
            persistDirectory(userId, "docs/sub/");
            persistFile(userId, "key2", "docs/sub/b.txt", 20);

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
            persistDirectory(userId, "docs/");
            persistDirectory(userId, "docs-backup/");

            // when
            repository.deleteByNormalizedPathPrefix(userId, "docs/");

            // then
            assertThat(repository.existsByNormalizedPath(userId, "docs-backup/")).isTrue();
        }
    }

    @Nested
    class DeleteByNormalizedPaths {

        @Test
        @DisplayName("Should delete multiple resources by normalized paths")
        void shouldDeleteMultiplePaths() {
            // given
            persistFile(userId, "key1", "a.txt", 10);
            persistFile(userId, "key2", "b.txt", 20);
            persistFile(userId, "key3", "c.txt", 30);

            // when
            repository.deleteByNormalizedPaths(userId, List.of("a.txt", "b.txt"));

            // then
            assertThat(repository.existsByNormalizedPath(userId, "a.txt")).isFalse();
            assertThat(repository.existsByNormalizedPath(userId, "b.txt")).isFalse();
            assertThat(repository.existsByNormalizedPath(userId, "c.txt")).isTrue();
        }
    }

    @Nested
    class DeleteByIds {

        @Test
        @DisplayName("Should delete resources by ids")
        void shouldDeleteByIds() {
            // given
            ResourceMetadata f1 = file(userId, "key1", "a.txt", 10);
            ResourceMetadata f2 = file(userId, "key2", "b.txt", 20);
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

    @Nested
    class MoveMetadata {

        @Test
        @DisplayName("Should correctly move file to another directory")
        void shouldMoveFileToAnotherDirectory() {
            // given
            persistDirectory(userId, "docs/");
            persistDirectory(userId, "work/");
            persistFile(userId, "key", "docs/file.txt", 10);

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
            persistDirectory(userId, "docs/");
            persistFile(userId, "key", "docs/file.txt", 10);

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
            persistDirectory(userId, "docs/");
            persistDirectory(userId, "archive/");

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
            persistDirectory(userId, "docs/");
            persistDirectory(userId, "archive/");
            persistFile(userId, "key1", "docs/a.txt", 10);
            persistFile(userId, "key2", "docs/b.txt", 20);

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
            persistDirectory(userId, "docs/");
            persistDirectory(userId, "docs/sub/");
            persistFile(userId, "key", "docs/sub/deep.txt", 10);
            persistDirectory(userId, "archive/");

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
            persistDirectory(userId, "docs/");

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
            persistDirectory(userId, "archive/");
            persistDirectory(userId, "archive/docs/");
            persistFile(userId, "key", "archive/docs/file.txt", 10);

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
            persistDirectory(userId, "docs/");
            persistDirectory(otherUserId, "docs/");
            persistFile(otherUserId, "key", "docs/file.txt", 10);

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
            persistDirectory(userId, "docs/");
            persistDirectory(userId, "docs-backup/");
            persistFile(userId, "key", "docs-backup/file.txt", 10);

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
            persistDirectory(userId, "docs/");
            persistFile(userId, "key", "docs/file.txt", 10);

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
}