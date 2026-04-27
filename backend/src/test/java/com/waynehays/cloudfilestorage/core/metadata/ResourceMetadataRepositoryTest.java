package com.waynehays.cloudfilestorage.core.metadata;

import com.waynehays.cloudfilestorage.core.metadata.dto.DirectoryRowDto;
import com.waynehays.cloudfilestorage.core.metadata.dto.FileRowDto;
import com.waynehays.cloudfilestorage.AbstractRepositoryTest;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

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

    @Nested
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

    @Nested
    class FindMissingPaths {

        @Test
        @DisplayName("Should return all paths when nothing of them found in database")
        void shouldReturnAllPaths_whenNoneExistInDatabase() {
            // given
            Set<String> candidates = Set.of("docs/", "docs/file.txt", "images/");

            // when
            Set<String> result = repository.findMissingPaths(userId, candidates);

            // then
            assertThat(result).containsExactlyInAnyOrderElementsOf(candidates);
        }

        @Test
        @DisplayName("Should return paths that not found in database")
        void shouldReturnOnlyMissingPaths_whenSomeAlreadyExist() {
            // given
            em.persistAndFlush(directory(userId, "docs/", "", "docs"));
            Set<String> candidates = Set.of("docs/", "images/");

            // when
            Set<String> result = repository.findMissingPaths(userId, candidates);

            // then
            assertThat(result).containsExactly("images/");
        }

        @Test
        @DisplayName("Should return empty set when all paths exists in database")
        void shouldReturnEmptySet_whenAllPathsExist() {
            // given
            em.persistAndFlush(directory(userId, "docs/", "", "docs"));
            em.persistAndFlush(file(userId, "docs/file.txt", "docs/", "file.txt", 100L));
            Set<String> candidates = Set.of("docs/", "docs/file.txt");

            // when
            Set<String> result = repository.findMissingPaths(userId, candidates);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return path when exists for another user")
        void shouldReturnPath_whenExistsForOtherUserOnly() {
            // given
            em.persistAndFlush(directory(otherUserId, "docs/", "", "docs"));
            Set<String> candidates = Set.of("docs/");

            // when
            Set<String> result = repository.findMissingPaths(userId, candidates);

            // then
            assertThat(result).containsExactly("docs/");
        }

        @Test
        @DisplayName("Should return path when exists but marked for deletion")
        void shouldReturnPath_whenExistsButMarkedForDeletion() {
            // given
            ResourceMetadata dir = directory(userId, "docs/", "", "docs");
            dir.setMarkedForDeletion(true);
            em.persistAndFlush(dir);
            Set<String> candidates = Set.of("docs/");

            // when
            Set<String> result = repository.findMissingPaths(userId, candidates);

            // then
            assertThat(result).containsExactly("docs/");
        }

        @Test
        @DisplayName("Should not find paths when case-sensitive")
        void shouldBeCaseInsensitive_whenPathDiffersInCase() {
            // given
            em.persistAndFlush(directory(userId, "docs/", "", "docs"));
            Set<String> candidates = Set.of("DOCS/");

            // when
            Set<String> result = repository.findMissingPaths(userId, candidates);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty set when candidates is empty")
        void shouldReturnEmptySet_whenCandidatesIsEmpty() {
            // given & when
            Set<String> result = repository.findMissingPaths(userId, Set.of());

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class BatchSaveDirectories {

        @Test
        @DisplayName("Should save all directories")
        void shouldSaveAllDirectories() {
            // given
            List<DirectoryRowDto> directories = List.of(
                    new DirectoryRowDto("docs/", "docs/", "", "docs"),
                    new DirectoryRowDto("images/", "images/", "", "images")
            );

            // when
            repository.batchSaveDirectories(userId, directories);
            em.flush();
            em.clear();

            // then
            List<ResourceMetadata> saved = getSavedList();
            assertThat(saved).hasSize(2);
            assertThat(saved).extracting(ResourceMetadata::getPath)
                    .containsExactlyInAnyOrder("docs/", "images/");
        }

        @Test
        @DisplayName("Should save directories with correct type")
        void shouldSaveWithCorrectType() {
            // given
            List<DirectoryRowDto> directories = List.of(
                    new DirectoryRowDto("docs/", "docs/", "", "docs")
            );

            // when
            repository.batchSaveDirectories(userId, directories);
            em.flush();
            em.clear();

            // then
            ResourceMetadata saved = getSavedSingle();
            assertThat(saved.getType()).isEqualTo(ResourceType.DIRECTORY);
            assertThat(saved.isMarkedForDeletion()).isFalse();
            assertThat(saved.getSize()).isZero();
        }

        @Test
        @DisplayName("Should ignore conflict when some directories already exist")
        void shouldIgnoreConflict_whenDirectoryAlreadyExists() {
            // given
            em.persistAndFlush(directory(userId, "docs/", "", "docs"));
            List<DirectoryRowDto> directories = List.of(
                    new DirectoryRowDto("docs/", "docs/", "", "docs")
            );

            // when & then
            assertThatNoException()
                    .isThrownBy(() -> repository.batchSaveDirectories(userId, directories));
        }

        @Test
        @DisplayName("Should do nothing when list is empty")
        void shouldDoNothing_whenListIsEmpty() {
            // given & when
            repository.batchSaveDirectories(userId, List.of());
            em.flush();

            // then
            List<ResourceMetadata> saved = getSavedList();
            assertThat(saved).isEmpty();
        }
    }

    @Nested
    class BatchSaveFiles {

        @Test
        @DisplayName("Should save all files")
        void shouldSaveAllFiles() {
            // given
            List<FileRowDto> files = List.of(
                    new FileRowDto("docs/a.txt", "docs/a.txt", "docs/", "a.txt", 100L),
                    new FileRowDto("docs/b.txt", "docs/b.txt", "docs/", "b.txt", 200L)
            );

            // when
            repository.batchSaveFiles(userId, files);
            em.flush();
            em.clear();

            // then
            List<ResourceMetadata> saved = getSavedList();
            assertThat(saved).hasSize(2);
            assertThat(saved).extracting(ResourceMetadata::getPath)
                    .containsExactlyInAnyOrder("docs/a.txt", "docs/b.txt");
        }

        @Test
        @DisplayName("Should save with correct type and size")
        void shouldSaveWithCorrectTypeAndSize() {
            // given
            List<FileRowDto> files = List.of(
                    new FileRowDto("docs/file.txt", "docs/file.txt", "docs/", "file.txt", 512L)
            );

            // when
            repository.batchSaveFiles(userId, files);
            em.flush();
            em.clear();

            // then
            ResourceMetadata saved = getSavedSingle();
            assertThat(saved.getType()).isEqualTo(ResourceType.FILE);
            assertThat(saved.getSize()).isEqualTo(512L);
            assertThat(saved.isMarkedForDeletion()).isFalse();
        }

        @Test
        @DisplayName("Should do nothing when list is empty")
        void shouldDoNothing_whenListIsEmpty() {
            // given & when
            repository.batchSaveFiles(userId, List.of());
            em.flush();

            // then
            List<ResourceMetadata> saved = getSavedList();
            assertThat(saved).isEmpty();
        }
    }

    @Nested
    class MarkForDeletionAndSumSize {

        @Test
        @DisplayName("Should mark files for deletion and return their summary size")
        void shouldMarkFilesAndReturnTotalSize() {
            // given
            em.persistAndFlush(file(userId, "docs/a.txt", "docs/", "a.txt", 100L));
            em.persistAndFlush(file(userId, "docs/b.txt", "docs/", "b.txt", 200L));

            // when
            long totalSize = repository.markForDeletionAndSumSize(userId, "docs/");
            em.flush();
            em.clear();

            // then
            assertThat(totalSize).isEqualTo(300L);
            List<ResourceMetadata> marked = getSavedList();
            assertThat(marked).allMatch(ResourceMetadata::isMarkedForDeletion);
        }

        @Test
        @DisplayName("Should mark only files under prefix when other files exists too")
        void shouldMarkOnlyFilesUnderPrefix_whenOtherFilesExist() {
            // given
            em.persistAndFlush(file(userId, "docs/file.txt", "docs/", "file.txt", 100L));
            em.persistAndFlush(file(userId, "images/photo.jpg", "images/", "photo.jpg", 500L));

            // when
            long totalSize = repository.markForDeletionAndSumSize(userId, "docs/");
            em.flush();
            em.clear();

            // then
            assertThat(totalSize).isEqualTo(100L);
            List<ResourceMetadata> all = getSavedList();
            assertThat(all).filteredOn(r -> r.getPath().startsWith("docs/"))
                    .allMatch(ResourceMetadata::isMarkedForDeletion);
            assertThat(all).filteredOn(r -> r.getPath().startsWith("images/"))
                    .noneMatch(ResourceMetadata::isMarkedForDeletion);
        }

        @Test
        @DisplayName("Should not mark directories when prefix matches for directories too")
        void shouldNotMarkDirectories_whenPrefixMatchesDirectoryToo() {
            // given
            em.persistAndFlush(directory(userId, "docs/", "", "docs"));
            em.persistAndFlush(file(userId, "docs/file.txt", "docs/", "file.txt", 100L));

            // when
            repository.markForDeletionAndSumSize(userId, "docs/");
            em.flush();
            em.clear();

            // then
            List<ResourceMetadata> all = getSavedList();
            assertThat(all).filteredOn(r -> r.getType() == ResourceType.DIRECTORY)
                    .noneMatch(ResourceMetadata::isMarkedForDeletion);
        }

        @Test
        @DisplayName("Should return zero when no files under prefix found")
        void shouldReturnZero_whenNoFilesUnderPrefix() {
            // given & when
            long totalSize = repository.markForDeletionAndSumSize(userId, "docs/");

            // then
            assertThat(totalSize).isZero();
        }

        @Test
        @DisplayName("Should not count already marked for deletion files when they match prefix too")
        void shouldNotCountAlreadyMarkedFiles_whenTheyMatchPrefix() {
            // given
            ResourceMetadata file = file(userId, "docs/old.txt", "docs/", "old.txt", 900L);
            file.setMarkedForDeletion(true);
            em.persistAndFlush(file);
            em.persistAndFlush(file(userId, "docs/new.txt", "docs/", "new.txt", 100L));

            // when
            long totalSize = repository.markForDeletionAndSumSize(userId, "docs/");

            // then
            assertThat(totalSize).isEqualTo(1000L);
        }
    }
}

