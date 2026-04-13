package com.waynehays.cloudfilestorage.integration.repository;

import com.waynehays.cloudfilestorage.dto.internal.metadata.DirectoryRow;
import com.waynehays.cloudfilestorage.dto.internal.metadata.FileRow;
import com.waynehays.cloudfilestorage.dto.internal.quota.UsedSpace;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.entity.ResourceType;
import com.waynehays.cloudfilestorage.repository.metadata.ResourceMetadataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResourceMetadataRepository integration tests")
class ResourceMetadataRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private ResourceMetadataRepository repository;

    private ResourceMetadata file(Long userId, String path, String parentPath, String name, long size) {
        ResourceMetadata r = new ResourceMetadata();
        r.setUserId(userId);
        r.setPath(path);
        r.setParentPath(parentPath);
        r.setName(name);
        r.setType(ResourceType.FILE);
        r.setSize(size);
        r.setMarkedForDeletion(false);
        return r;
    }

    private ResourceMetadata directory(Long userId, String path, String parentPath, String name) {
        ResourceMetadata r = new ResourceMetadata();
        r.setUserId(userId);
        r.setPath(path);
        r.setParentPath(parentPath);
        r.setName(name);
        r.setType(ResourceType.DIRECTORY);
        r.setSize(0L);
        r.setMarkedForDeletion(false);
        return r;
    }

    @Nested
    @DisplayName("findByPath")
    class FindByPath {

        @Test
        @DisplayName("should return resource by exact path")
        void shouldReturnByPath() {
            // given
            String path = "docs/a.txt";
            String name = "a.txt";
            em.persistAndFlush(file(userId, path, "docs/", name, 100));

            // when
            Optional<ResourceMetadata> result = repository.findByPath(userId, path);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo(name);
        }

        @Test
        @DisplayName("should return empty when marked for deletion")
        void shouldIgnoreMarkedForDeletion() {
            // given
            String path = "docs/a.txt";
            ResourceMetadata r = file(userId, path, "docs/", "a.txt", 100);
            r.setMarkedForDeletion(true);
            em.persistAndFlush(r);

            // when
            Optional<ResourceMetadata> result = repository.findByPath(userId, path);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should not return other user's resource")
        void shouldRespectUserIsolation() {
            // given
            String path = "docs/a.txt";
            em.persistAndFlush(file(otherUserId, path, "docs/", "a.txt", 100));

            // when
            Optional<ResourceMetadata> result = repository.findByPath(userId, path);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByParentPath")
    class FindByParentPath {

        @Test
        @DisplayName("should return only direct children")
        void shouldReturnDirectChildren() {
            // given
            em.persist(file(userId, "docs/a.txt", "docs/", "a.txt", 10));
            em.persist(file(userId, "docs/b.txt", "docs/", "b.txt", 20));
            em.persist(file(userId, "docs/sub/c.txt", "docs/sub/", "c.txt", 30));
            em.flush();

            // when
            List<ResourceMetadata> result = repository.findByParentPath(userId, "docs/");

            // then
            assertThat(result).extracting(ResourceMetadata::getName)
                    .containsExactlyInAnyOrder("a.txt", "b.txt");
        }

        @Test
        @DisplayName("should exclude marked for deletion")
        void shouldExcludeMarked() {
            // given
            em.persist(file(userId, "docs/a.txt", "docs/", "a.txt", 10));
            ResourceMetadata marked = file(userId, "docs/b.txt", "docs/", "b.txt", 20);
            marked.setMarkedForDeletion(true);
            em.persist(marked);
            em.flush();

            // when
            List<ResourceMetadata> result = repository.findByParentPath(userId, "docs/");

            // then
            assertThat(result).extracting(ResourceMetadata::getName).containsExactly("a.txt");
        }
    }

    @Nested
    @DisplayName("findByNameContaining")
    class FindByNameContaining {

        @Test
        @DisplayName("should match case-insensitively")
        void shouldMatchCaseInsensitive() {
            // given
            em.persist(file(userId, "Report.PDF", "", "Report.PDF", 10));
            em.persist(file(userId, "notes.txt", "", "notes.txt", 10));
            em.flush();

            // when
            List<ResourceMetadata> result = repository.findByNameContaining(userId, "report", PageRequest.of(0, 10));

            // then
            assertThat(result).extracting(ResourceMetadata::getName).containsExactly("Report.PDF");
        }

        @Test
        @DisplayName("should use pageable limit")
        void shouldUsePageable() {
            // given
            for (int i = 0; i < 5; i++) {
                em.persist(file(userId, "file" + i + ".txt", "", "file" + i + ".txt", 10));
            }
            em.flush();

            // when
            List<ResourceMetadata> result = repository.findByNameContaining(userId, "file", PageRequest.of(0, 2));

            // then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("findFilesMarkedForDeletion")
    class FindFilesMarkedForDeletion {

        @Test
        @DisplayName("Should return only files marked for deletion")
        void shouldFindFilesMarkedForDeletion() {
            // given
            ResourceMetadata file1 = file(userId, "docs/a.txt", "docs/", "a.txt", 10);
            ResourceMetadata file2 = file(userId, "docs/sub/b.txt", "docs/sub/", "b.txt", 20);
            ResourceMetadata file3 = file(userId, "other/c.txt", "other/", "c.txt", 30);
            ResourceMetadata directory = directory(userId, "docs/sub/", "docs/", "sub");

            file1.setMarkedForDeletion(true);
            file2.setMarkedForDeletion(true);
            directory.setMarkedForDeletion(true);

            em.persist(file1);
            em.persist(file2);
            em.persist(directory);
            em.persist(file3);
            em.flush();

            // when
            List<ResourceMetadata> result = repository.findFilesMarkedForDeletion(Pageable.ofSize(5));

            // then
            assertThat(result).hasSize(2);
            assertThat(result).contains(file1).contains(file2);
        }
    }

    @Nested
    @DisplayName("findFilesByPathPrefix")
    class FindFilesByPathPrefix {

        @Test
        @DisplayName("should return only files under prefix")
        void shouldReturnFilesUnderPrefix() {
            // given
            em.persist(file(userId, "docs/a.txt", "docs/", "a.txt", 10));
            em.persist(file(userId, "docs/sub/b.txt", "docs/sub/", "b.txt", 20));
            em.persist(directory(userId, "docs/sub/", "docs/", "sub"));
            em.persist(file(userId, "other/c.txt", "other/", "c.txt", 30));
            em.flush();

            // when
            List<ResourceMetadata> result = repository.findFilesByPathPrefix(userId, "docs/");

            // then
            assertThat(result).extracting(ResourceMetadata::getName)
                    .containsExactlyInAnyOrder("a.txt", "b.txt");
        }
    }

    @Nested
    @DisplayName("findExistingPaths")
    class FindExistingPaths {

        @Test
        @DisplayName("should return only existing paths")
        void shouldReturnExisting() {
            // given
            em.persist(file(userId, "a.txt", "", "a.txt", 10));
            em.persist(file(userId, "b.txt", "", "b.txt", 10));
            em.flush();

            // when
            Set<String> result = repository.findExistingPaths(userId, Set.of("a.txt", "b.txt", "c.txt"));

            // then
            assertThat(result).containsExactlyInAnyOrder("a.txt", "b.txt");
        }
    }

    @Nested
    @DisplayName("findMissingPaths")
    class FindMissingPaths {

        @Test
        @DisplayName("should return paths that do not exist for user")
        void shouldReturnMissing() {
            // given
            repository.saveDirectories(userId, List.of(
                    new DirectoryRow("docs/", "", "docs"),
                    new DirectoryRow("docs/sub/", "docs/", "sub")
            ));

            // when
            Set<String> missing = repository.findMissingPaths(
                    userId,
                    Set.of("docs/", "docs/sub/", "docs/new/", "other/")
            );

            // then
            assertThat(missing).containsExactlyInAnyOrder("docs/new/", "other/");
        }

        @Test
        @DisplayName("should return all paths when nothing exists")
        void shouldReturnAllWhenEmpty() {
            // given
            Set<String> candidates = Set.of("a/", "b/", "c/");

            // when
            Set<String> missing = repository.findMissingPaths(userId, candidates);

            // then
            assertThat(missing).containsExactlyInAnyOrderElementsOf(candidates);
        }

        @Test
        @DisplayName("should return empty when all paths exist")
        void shouldReturnEmptyWhenAllExist() {
            // given
            repository.saveDirectories(userId, List.of(
                    new DirectoryRow("docs/", "", "docs")
            ));

            // when
            Set<String> missing = repository.findMissingPaths(userId, Set.of("docs/"));

            // then
            assertThat(missing).isEmpty();
        }

        @Test
        @DisplayName("should not consider other user's paths as existing")
        void shouldRespectUserIsolation() {
            // given
            repository.saveDirectories(otherUserId, List.of(
                    new DirectoryRow("docs/", "", "docs")
            ));

            // when
            Set<String> missing = repository.findMissingPaths(userId, Set.of("docs/"));

            // then
            assertThat(missing).containsExactly("docs/");
        }

        @Test
        @DisplayName("should ignore marked for deletion paths")
        void shouldIgnoreMarkedForDeletion() {
            // given
            repository.saveDirectories(userId, List.of(
                    new DirectoryRow("docs/", "", "docs")
            ));
            repository.markForDeletionByPath(userId, "docs/");
            em.clear();

            // when
            Set<String> missing = repository.findMissingPaths(userId, Set.of("docs/"));

            // then
            assertThat(missing).containsExactly("docs/");
        }

        @Test
        @DisplayName("should return empty set on empty input")
        void shouldReturnEmptyOnEmptyInput() {
            // given

            // when
            Set<String> missing = repository.findMissingPaths(userId, Set.of());

            // then
            assertThat(missing).isEmpty();
        }
    }

    @Nested
    @DisplayName("sumFileSizesGroupByUserId")
    class SumFileSizes {

        @Test
        @DisplayName("should sum file sizes per user")
        void shouldSumPerUser() {
            // given
            em.persist(file(userId, "a.txt", "", "a.txt", 100));
            em.persist(file(userId, "b.txt", "", "b.txt", 200));
            em.persist(file(otherUserId, "c.txt", "", "c.txt", 50));
            em.persist(directory(userId, "dir/", "", "dir"));
            em.flush();

            // when
            List<UsedSpace> result = repository.sumFileSizesGroupByUserId(
                    List.of(userId, otherUserId), ResourceType.FILE);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).anySatisfy(u -> {
                assertThat(u.getUserId()).isEqualTo(userId);
                assertThat(u.getTotalSize()).isEqualTo(300);
            });
            assertThat(result).anySatisfy(u -> {
                assertThat(u.getUserId()).isEqualTo(otherUserId);
                assertThat(u.getTotalSize()).isEqualTo(50);
            });
        }
    }

    @Nested
    @DisplayName("markForDeletionByPath")
    class MarkForDeletion {

        @Test
        @DisplayName("should mark resource for deletion")
        void shouldMark() {
            // given
            em.persist(file(userId, "a.txt", "", "a.txt", 100));
            em.flush();

            // when
            repository.markForDeletionByPath(userId, "a.txt");
            em.clear();

            // then
            assertThat(repository.findByPath(userId, "a.txt")).isEmpty();
        }
    }

    @Nested
    @DisplayName("updatePathsByPathPrefix")
    class UpdatePaths {

        @Test
        @DisplayName("should rewrite path and parentPath for all matching resources")
        void shouldRewritePaths() {
            // given
            em.persist(directory(userId, "old/", "", "old"));
            em.persist(file(userId, "old/a.txt", "old/", "a.txt", 10));
            em.persist(file(userId, "old/sub/b.txt", "old/sub/", "b.txt", 20));
            em.persist(directory(userId, "old/sub/", "old/", "sub"));
            em.flush();

            // when
            repository.updatePathsByPathPrefix(userId, "old/", "new/");
            em.clear();

            // then
            assertThat(repository.findByPath(userId, "new/a.txt")).isPresent();
            assertThat(repository.findByPath(userId, "new/sub/b.txt")).isPresent();
            assertThat(repository.findByPath(userId, "old/a.txt")).isEmpty();

            ResourceMetadata movedFile = repository.findByPath(userId, "new/sub/b.txt").orElseThrow();
            assertThat(movedFile.getParentPath()).isEqualTo("new/sub/");
        }

        @Test
        @DisplayName("should not affect other user's resources")
        void shouldNotAffectOtherUsers() {
            // given
            em.persist(file(userId, "old/a.txt", "old/", "a.txt", 10));
            em.persist(file(otherUserId, "old/a.txt", "old/", "a.txt", 10));
            em.flush();

            // when
            repository.updatePathsByPathPrefix(userId, "old/", "new/");
            em.clear();

            // then
            assertThat(repository.findByPath(otherUserId, "old/a.txt")).isPresent();
            assertThat(repository.findByPath(otherUserId, "new/a.txt")).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteByPathPrefix")
    class DeleteByPathPrefix {

        @Test
        @DisplayName("should delete all resources under prefix")
        void shouldDeleteByPrefix() {
            // given
            em.persist(file(userId, "docs/a.txt", "docs/", "a.txt", 10));
            em.persist(file(userId, "docs/sub/b.txt", "docs/sub/", "b.txt", 20));
            em.persist(file(userId, "other/c.txt", "other/", "c.txt", 30));
            em.flush();

            // when
            repository.deleteByPathPrefix(userId, "docs/");
            em.clear();

            // then
            assertThat(repository.findByPath(userId, "docs/a.txt")).isEmpty();
            assertThat(repository.findByPath(userId, "docs/sub/b.txt")).isEmpty();
            assertThat(repository.findByPath(userId, "other/c.txt")).isPresent();
        }
    }

    @Nested
    @DisplayName("deleteByPaths")
    class DeleteByPaths {

        @Test
        @DisplayName("Should delete all resources by paths")
        void shouldDeleteAllResourcesByPaths() {
            // given
            String path1 = "docs/a.txt";
            String path2 = "docs/sub/b.txt";
            String path3 = "other/c.txt";
            em.persist(file(userId, path1, "docs/", "a.txt", 10));
            em.persist(file(userId, path2, "docs/sub/", "b.txt", 20));
            em.persist(file(userId, path3, "other/", "c.txt", 30));
            em.flush();

            // when
            repository.deleteByPaths(userId, List.of(path1, path2, path3));
            em.clear();

            // then
            assertThat(repository.findByPath(userId, path1)).isEmpty();
            assertThat(repository.findByPath(userId, path2)).isEmpty();
            assertThat(repository.findByPath(userId, path3)).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteByIds")
    class DeleteByIds {

        @Test
        @DisplayName("Should delete all resources by ids")
        void shouldDeleteAllResourcesByIds() {
            // given
            String path1 = "docs/a.txt";
            String path2 = "docs/sub/b.txt";
            String path3 = "other/c.txt";
            ResourceMetadata file1 = file(userId, path1, "docs/", "a.txt", 10);
            ResourceMetadata file2 = file(otherUserId, path2, "docs/sub/", "b.txt", 20);
            ResourceMetadata file3 = file(userId, path3, "other/", "c.txt", 30);
            em.persist(file1);
            em.persist(file2);
            em.persist(file3);
            em.flush();

            // when
            repository.deleteByIds(List.of(file1.getId(), file2.getId(), file3.getId()));
            em.clear();

            // then
            assertThat(repository.findByPath(userId, path1)).isEmpty();
            assertThat(repository.findByPath(userId, path2)).isEmpty();
            assertThat(repository.findByPath(userId, path3)).isEmpty();
        }
    }

    @Nested
    @DisplayName("saveDirectories")
    class SaveDirectories {

        @Test
        @DisplayName("should batch insert directories")
        void shouldBatchInsert() {
            // given
            List<DirectoryRow> directories = List.of(
                    new DirectoryRow("docs/", "", "docs"),
                    new DirectoryRow("docs/sub/", "docs/", "sub")
            );

            // when
            repository.saveDirectories(userId, directories);
            em.clear();

            // then
            assertThat(repository.findByPath(userId, "docs/")).isPresent();
            assertThat(repository.findByPath(userId, "docs/sub/")).isPresent();
        }

        @Test
        @DisplayName("should not overwrite existing row on duplicate path")
        void shouldIgnoreConflicts() {
            // given
            repository.saveDirectories(userId, List.of(new DirectoryRow("docs/", "", "original-name")));

            // when
            repository.saveDirectories(userId, List.of(
                    new DirectoryRow("docs/", "", "overwritten-name"),
                    new DirectoryRow("other/", "", "other")));
            em.clear();

            // then
            ResourceMetadata existing = repository.findByPath(userId, "docs/").orElseThrow();
            assertThat(existing.getName()).isEqualTo("original-name");
            assertThat(repository.findByPath(userId, "other/")).isPresent();
        }

        @Test
        @DisplayName("should allow same path for different users")
        void shouldAllowSamePathDifferentUsers() {
            // given
            repository.saveDirectories(userId, List.of(new DirectoryRow("docs/", "", "docs")));

            // when
            repository.saveDirectories(otherUserId, List.of(new DirectoryRow("docs/", "", "docs")));
            em.clear();

            // then
            assertThat(repository.findByPath(userId, "docs/")).isPresent();
            assertThat(repository.findByPath(otherUserId, "docs/")).isPresent();
        }
    }

    @Nested
    @DisplayName("saveFiles")
    class SaveFiles {

        @Test
        @DisplayName("should batch insert files")
        void shouldBatchInsert() {
            // given
            List<FileRow> files = List.of(
                    new FileRow("a.txt", "", "a.txt", 100L),
                    new FileRow("docs/b.txt", "docs/", "b.txt", 200L)
            );

            // when
            repository.saveFiles(userId, files);
            em.clear();

            // then
            ResourceMetadata a = repository.findByPath(userId, "a.txt").orElseThrow();
            assertThat(a.getSize()).isEqualTo(100);
            assertThat(a.getType()).isEqualTo(ResourceType.FILE);

            ResourceMetadata b = repository.findByPath(userId, "docs/b.txt").orElseThrow();
            assertThat(b.getSize()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("markForDeletionAndSumSize")
    class MarkAndSum {

        @Test
        @DisplayName("should mark files under prefix and return total size")
        void shouldMarkAndSum() {
            // given
            repository.saveFiles(userId, List.of(
                    new FileRow("docs/a.txt", "docs/", "a.txt", 100L),
                    new FileRow("docs/b.txt", "docs/", "b.txt", 200L),
                    new FileRow("docs/sub/c.txt", "docs/sub/", "c.txt", 50L),
                    new FileRow("other/d.txt", "other/", "d.txt", 999L)
            ));

            // when
            long totalSize = repository.markForDeletionAndSumSize(userId, "docs/");
            em.clear();

            // then
            assertThat(totalSize).isEqualTo(350L);
            assertThat(repository.findByPath(userId, "docs/a.txt")).isEmpty();
            assertThat(repository.findByPath(userId, "docs/b.txt")).isEmpty();
            assertThat(repository.findByPath(userId, "docs/sub/c.txt")).isEmpty();
            assertThat(repository.findByPath(userId, "other/d.txt")).isPresent();
        }

        @Test
        @DisplayName("should return zero when nothing matches")
        void shouldReturnZeroWhenEmpty() {
            // given
            repository.saveFiles(userId, List.of(new FileRow("other/a.txt", "other/", "a.txt", 100L)
            ));

            // when
            long totalSize = repository.markForDeletionAndSumSize(userId, "docs/");

            // then
            assertThat(totalSize).isZero();
            assertThat(repository.findByPath(userId, "other/a.txt")).isPresent();
        }

        @Test
        @DisplayName("should not affect other users")
        void shouldNotAffectOtherUsers() {
            // given
            repository.saveFiles(userId, List.of(new FileRow("docs/a.txt", "docs/", "a.txt", 100L)));
            repository.saveFiles(otherUserId, List.of(new FileRow("docs/a.txt", "docs/", "a.txt", 100L)));

            // when
            long totalSize = repository.markForDeletionAndSumSize(userId, "docs/");
            em.clear();

            // then
            assertThat(totalSize).isEqualTo(100L);
            assertThat(repository.findByPath(otherUserId, "docs/a.txt")).isPresent();
        }

        @Test
        @DisplayName("should ignore directories")
        void shouldIgnoreDirectories() {
            // given
            repository.saveDirectories(userId, List.of(new DirectoryRow("docs/", "", "docs")));
            repository.saveFiles(userId, List.of(new FileRow("docs/a.txt", "docs/", "a.txt", 100L)));

            // when
            long totalSize = repository.markForDeletionAndSumSize(userId, "docs/");
            em.clear();

            // then
            assertThat(totalSize).isEqualTo(100L);
            assertThat(repository.findByPath(userId, "docs/")).isPresent();
        }
    }
}
