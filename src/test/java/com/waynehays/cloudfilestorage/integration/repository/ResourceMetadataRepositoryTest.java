package com.waynehays.cloudfilestorage.integration.repository;

import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.entity.User;
import com.waynehays.cloudfilestorage.integration.base.AbstractRepositoryBaseTest;
import com.waynehays.cloudfilestorage.repository.ResourceMetadataRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceMetadataRepositoryTest extends AbstractRepositoryBaseTest {

    @Autowired
    private ResourceMetadataRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Nested
    class FindByUserIdAndPath {

        @Test
        void shouldFindResourceByPath() {
            // given
            User user = persistUser();
            ResourceMetadata metadata = persistMetadata(user.getId(), "docs/file.txt", "docs/",
                    "file.txt", ResourceType.FILE, false);

            // when
            Optional<ResourceMetadata> result = repository.findByUserIdAndPathAndMarkedForDeletionFalse(
                    user.getId(), "docs/file.txt");

            // then
            assertThat(result).isPresent().contains(metadata);
        }

        @Test
        void shouldReturnEmptyForNonExistentPath() {
            // given
            User user = persistUser();

            // when
            Optional<ResourceMetadata> result = repository.findByUserIdAndPathAndMarkedForDeletionFalse(
                    user.getId(), "docs/file.txt");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldNotFindMarkedForDeletion() {
            // given
            User user = persistUser();
            persistMetadata(user.getId(), "docs/file.txt", "docs/",
                    "file.txt", ResourceType.FILE, true);

            // when
            Optional<ResourceMetadata> result = repository.findByUserIdAndPathAndMarkedForDeletionFalse(
                    user.getId(), "docs/file.txt");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldNotFindResourceOfAnotherUser() {
            // given
            User user1 = persistUser("user1");
            User user2 = persistUser("user2");
            persistMetadata(user1.getId(), "docs/file.txt", "docs/",
                    "file.txt", ResourceType.FILE, false);

            // when
            Optional<ResourceMetadata> result = repository.findByUserIdAndPathAndMarkedForDeletionFalse(
                    user2.getId(), "docs/file.txt");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindByPathPrefix {

        @Test
        void shouldReturnAllResourcesByPrefixRecursively() {
            // given
            User user = persistUser();
            ResourceMetadata file1 = persistMetadata(user.getId(), "docs/file1.txt", "docs/",
                    "file1.txt", ResourceType.FILE, false);
            ResourceMetadata file2 = persistMetadata(user.getId(), "docs/sub/file2.txt", "docs/sub/",
                    "file2.txt", ResourceType.FILE, false);

            // when
            List<ResourceMetadata> result = repository.findByUserIdAndPathStartingWithAndMarkedForDeletionFalse(
                    user.getId(), "docs/");

            // then
            assertThat(result).containsExactlyInAnyOrder(file1, file2);
        }

        @Test
        void shouldNotReturnMarkedForDeletion() {
            // given
            User user = persistUser();
            persistMetadata(user.getId(), "docs/file1.txt", "docs/",
                    "file1.txt", ResourceType.FILE, false);
            persistMetadata(user.getId(), "docs/file2.txt", "docs/",
                    "file2.txt", ResourceType.FILE, true);

            // when
            List<ResourceMetadata> result = repository.findByUserIdAndPathStartingWithAndMarkedForDeletionFalse(
                    user.getId(), "docs/");

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        void shouldReturnEmptyForNonExistentPrefix() {
            // given
            User user = persistUser();

            // when
            List<ResourceMetadata> result = repository.findByUserIdAndPathStartingWithAndMarkedForDeletionFalse(
                    user.getId(), "docs/");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldNotReturnResourcesOfAnotherUser() {
            // given
            User user1 = persistUser("user1");
            User user2 = persistUser("user2");
            persistMetadata(user1.getId(), "docs/file.txt", "docs/",
                    "file.txt", ResourceType.FILE, false);

            // when
            List<ResourceMetadata> result = repository.findByUserIdAndPathStartingWithAndMarkedForDeletionFalse(
                    user2.getId(), "docs/");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindByParentPath {

        @Test
        void shouldReturnOnlyDirectChildren() {
            // given
            User user = persistUser();
            ResourceMetadata file = persistMetadata(user.getId(), "docs/file.txt", "docs/",
                    "file.txt", ResourceType.FILE, false);
            ResourceMetadata subDir = persistMetadata(user.getId(), "docs/sub/", "docs/",
                    "sub", ResourceType.DIRECTORY, false);
            persistMetadata(user.getId(), "docs/sub/nested.txt", "docs/sub/",
                    "nested.txt", ResourceType.FILE, false);

            // when
            List<ResourceMetadata> result = repository.findByUserIdAndParentPathAndMarkedForDeletionFalse(
                    user.getId(), "docs/");

            // then
            assertThat(result).containsExactlyInAnyOrder(file, subDir);
        }

        @Test
        void shouldNotReturnMarkedForDeletion() {
            // given
            User user = persistUser();
            persistMetadata(user.getId(), "docs/file.txt", "docs/",
                    "file.txt", ResourceType.FILE, false);
            persistMetadata(user.getId(), "docs/deleted.txt", "docs/",
                    "deleted.txt", ResourceType.FILE, true);

            // when
            List<ResourceMetadata> result = repository.findByUserIdAndParentPathAndMarkedForDeletionFalse(
                    user.getId(), "docs/");

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        void shouldReturnEmptyForEmptyDirectory() {
            // given
            User user = persistUser();
            persistMetadata(user.getId(), "docs/", "",
                    "docs", ResourceType.DIRECTORY, false);

            // when
            List<ResourceMetadata> result = repository.findByUserIdAndParentPathAndMarkedForDeletionFalse(
                    user.getId(), "docs/");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindByNameContaining {

        @Test
        void shouldFindByPartialName() {
            // given
            User user = persistUser();
            ResourceMetadata match = persistMetadata(user.getId(), "docs/report.pdf", "docs/",
                    "report.pdf", ResourceType.FILE, false);
            persistMetadata(user.getId(), "docs/photo.png", "docs/",
                    "photo.png", ResourceType.FILE, false);

            // when
            List<ResourceMetadata> result = repository.findByUserIdAndNameContainingIgnoreCaseAndMarkedForDeletionFalse(
                    user.getId(), "report");

            // then
            assertThat(result).containsExactly(match);
        }

        @Test
        void shouldSearchCaseInsensitively() {
            // given
            User user = persistUser();
            ResourceMetadata match = persistMetadata(user.getId(), "docs/Report.pdf", "docs/",
                    "Report.pdf", ResourceType.FILE, false);

            // when
            List<ResourceMetadata> result = repository.findByUserIdAndNameContainingIgnoreCaseAndMarkedForDeletionFalse(
                    user.getId(), "REPORT");

            // then
            assertThat(result).containsExactly(match);
        }

        @Test
        void shouldNotReturnMarkedForDeletion() {
            // given
            User user = persistUser();
            persistMetadata(user.getId(), "docs/report.pdf", "docs/",
                    "report.pdf", ResourceType.FILE, true);

            // when
            List<ResourceMetadata> result = repository.findByUserIdAndNameContainingIgnoreCaseAndMarkedForDeletionFalse(
                    user.getId(), "report");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyWhenNoMatch() {
            // given
            User user = persistUser();
            persistMetadata(user.getId(), "docs/file.txt", "docs/",
                    "file.txt", ResourceType.FILE, false);

            // when
            List<ResourceMetadata> result = repository.findByUserIdAndNameContainingIgnoreCaseAndMarkedForDeletionFalse(
                    user.getId(), "nonexistent");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class ExistsByPath {

        @Test
        void shouldReturnTrueWhenExists() {
            // given
            User user = persistUser();
            persistMetadata(user.getId(), "docs/file.txt", "docs/",
                    "file.txt", ResourceType.FILE, false);

            // when
            boolean result = repository.existsByUserIdAndPathAndMarkedForDeletionFalse(
                    user.getId(), "docs/file.txt");

            // then
            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnFalseWhenNotExists() {
            // given
            User user = persistUser();

            // when
            boolean result = repository.existsByUserIdAndPathAndMarkedForDeletionFalse(
                    user.getId(), "docs/file.txt");

            // then
            assertThat(result).isFalse();
        }

        @Test
        void shouldReturnFalseWhenMarkedForDeletion() {
            // given
            User user = persistUser();
            persistMetadata(user.getId(), "docs/file.txt", "docs/",
                    "file.txt", ResourceType.FILE, true);

            // when
            boolean result = repository.existsByUserIdAndPathAndMarkedForDeletionFalse(
                    user.getId(), "docs/file.txt");

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    class FindMarkedForDeletion {

        @Test
        void shouldReturnAllMarked() {
            // given
            User user = persistUser();
            ResourceMetadata marked = persistMetadata(user.getId(), "docs/file.txt", "docs/",
                    "file.txt", ResourceType.FILE, true);
            persistMetadata(user.getId(), "docs/other.txt", "docs/",
                    "other.txt", ResourceType.FILE, false);

            // when
            List<ResourceMetadata> result = repository.findByMarkedForDeletionTrue();

            // then
            assertThat(result).containsExactly(marked);
        }

        @Test
        void shouldReturnEmptyWhenNoneMarked() {
            // given
            User user = persistUser();
            persistMetadata(user.getId(), "docs/file.txt", "docs/",
                    "file.txt", ResourceType.FILE, false);

            // when
            List<ResourceMetadata> result = repository.findByMarkedForDeletionTrue();

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class DeleteByPath {

        @Test
        void shouldDeleteByExactPath() {
            // given
            User user = persistUser();
            persistMetadata(user.getId(), "docs/file.txt", "docs/",
                    "file.txt", ResourceType.FILE, false);

            // when
            repository.deleteByUserIdAndPath(user.getId(), "docs/file.txt");
            flushAndClear();

            // then
            Optional<ResourceMetadata> result = repository.findByUserIdAndPathAndMarkedForDeletionFalse(
                    user.getId(), "docs/file.txt");
            assertThat(result).isEmpty();
        }

        @Test
        void shouldNotDeleteOtherPaths() {
            // given
            User user = persistUser();
            persistMetadata(user.getId(), "docs/file.txt", "docs/",
                    "file.txt", ResourceType.FILE, false);
            ResourceMetadata other = persistMetadata(user.getId(), "docs/other.txt", "docs/",
                    "other.txt", ResourceType.FILE, false);

            // when
            repository.deleteByUserIdAndPath(user.getId(), "docs/file.txt");
            flushAndClear();

            // then
            Optional<ResourceMetadata> result = repository.findByUserIdAndPathAndMarkedForDeletionFalse(
                    user.getId(), "docs/other.txt");
            assertThat(result).isPresent().contains(other);
        }
    }

    @Nested
    class DeleteByPrefix {

        @Test
        void shouldDeleteAllByPrefix() {
            // given
            User user = persistUser();
            persistMetadata(user.getId(), "docs/file1.txt", "docs/",
                    "file1.txt", ResourceType.FILE, false);
            persistMetadata(user.getId(), "docs/sub/file2.txt", "docs/sub/",
                    "file2.txt", ResourceType.FILE, false);

            // when
            repository.deleteByUserIdAndPathStartingWith(user.getId(), "docs/");
            flushAndClear();

            // then
            List<ResourceMetadata> result = repository.findByUserIdAndPathStartingWithAndMarkedForDeletionFalse(
                    user.getId(), "docs/");
            assertThat(result).isEmpty();
        }

        @Test
        void shouldNotDeleteOtherPrefixes() {
            // given
            User user = persistUser();
            persistMetadata(user.getId(), "docs/file.txt", "docs/",
                    "file.txt", ResourceType.FILE, false);
            ResourceMetadata other = persistMetadata(user.getId(), "images/photo.png", "images/",
                    "photo.png", ResourceType.FILE, false);

            // when
            repository.deleteByUserIdAndPathStartingWith(user.getId(), "docs/");
            flushAndClear();

            // then
            Optional<ResourceMetadata> result = repository.findByUserIdAndPathAndMarkedForDeletionFalse(
                    user.getId(), "images/photo.png");
            assertThat(result).isPresent().contains(other);
        }
    }

    @Nested
    class MarkForDeletion {

        @Test
        void shouldMarkByExactPath() {
            // given
            User user = persistUser();
            persistMetadata(user.getId(), "docs/file.txt", "docs/",
                    "file.txt", ResourceType.FILE, false);

            // when
            repository.markForDeletion(user.getId(), "docs/file.txt");
            flushAndClear();

            // then
            Optional<ResourceMetadata> result = repository.findByUserIdAndPathAndMarkedForDeletionFalse(
                    user.getId(), "docs/file.txt");
            assertThat(result).isEmpty();
        }

        @Test
        void shouldNotMarkOtherPaths() {
            // given
            User user = persistUser();
            persistMetadata(user.getId(), "docs/file.txt", "docs/",
                    "file.txt", ResourceType.FILE, false);
            persistMetadata(user.getId(), "docs/other.txt", "docs/",
                    "other.txt", ResourceType.FILE, false);

            // when
            repository.markForDeletion(user.getId(), "docs/file.txt");
            flushAndClear();

            // then
            Optional<ResourceMetadata> result = repository.findByUserIdAndPathAndMarkedForDeletionFalse(
                    user.getId(), "docs/other.txt");
            assertThat(result).isPresent();
        }
    }

    @Nested
    class MarkForDeletionByPrefix {

        @Test
        void shouldMarkAllByPrefix() {
            // given
            User user = persistUser();
            persistMetadata(user.getId(), "docs/file1.txt", "docs/",
                    "file1.txt", ResourceType.FILE, false);
            persistMetadata(user.getId(), "docs/sub/file2.txt", "docs/sub/",
                    "file2.txt", ResourceType.FILE, false);

            // when
            repository.markForDeletionByPrefix(user.getId(), "docs/");
            flushAndClear();

            // then
            List<ResourceMetadata> result = repository.findByUserIdAndPathStartingWithAndMarkedForDeletionFalse(
                    user.getId(), "docs/");
            assertThat(result).isEmpty();
        }

        @Test
        void shouldNotMarkOtherPrefixes() {
            // given
            User user = persistUser();
            persistMetadata(user.getId(), "docs/file.txt", "docs/",
                    "file.txt", ResourceType.FILE, false);
            persistMetadata(user.getId(), "images/photo.png", "images/",
                    "photo.png", ResourceType.FILE, false);

            // when
            repository.markForDeletionByPrefix(user.getId(), "docs/");
            flushAndClear();

            // then
            Optional<ResourceMetadata> result = repository.findByUserIdAndPathAndMarkedForDeletionFalse(
                    user.getId(), "images/photo.png");
            assertThat(result).isPresent();
        }
    }

    private User persistUser() {
        return persistUser("testuser");
    }

    private User persistUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("password");
        return entityManager.persistAndFlush(user);
    }

    private ResourceMetadata persistMetadata(Long userId, String path, String parentPath,
                                             String name, ResourceType type, boolean markedForDeletion) {
        ResourceMetadata metadata = new ResourceMetadata();
        metadata.setUserId(userId);
        metadata.setPath(path);
        metadata.setParentPath(parentPath);
        metadata.setName(name);
        metadata.setType(type);
        metadata.setSize(type == ResourceType.FILE ? 100L : null);
        metadata.setMarkedForDeletion(markedForDeletion);
        return entityManager.persistAndFlush(metadata);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
