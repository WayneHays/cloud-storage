package com.waynehays.cloudfilestorage.integration.repository;

import com.waynehays.cloudfilestorage.entity.User;
import com.waynehays.cloudfilestorage.integration.base.AbstractRepositoryBaseTest;
import com.waynehays.cloudfilestorage.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserRepository integration tests")
class UserRepositoryTest extends AbstractRepositoryBaseTest {

    @Autowired
    private UserRepository repository;

    @Nested
    @DisplayName("findByUsername")
    class FindByUsername {

        @Test
        @DisplayName("should return user by username")
        void shouldReturnByUsername() {
            // when
            Optional<User> result = repository.findByUsername("user");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getUsername()).isEqualTo("user");
        }

        @Test
        @DisplayName("should return empty when username not found")
        void shouldReturnEmptyWhenNotFound() {
            // when
            Optional<User> result = repository.findByUsername("nonexistent");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should be case-sensitive")
        void shouldBeCaseSensitive() {
            // when
            Optional<User> result = repository.findByUsername("USER");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll with pageable")
    class FindAllPageable {

        @Test
        @DisplayName("should return paginated users")
        void shouldReturnPage() {
            // when
            Page<User> page = repository.findAll(PageRequest.of(0, 10));

            // then
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getContent()).extracting(User::getUsername)
                    .containsExactlyInAnyOrder("user", "other");
        }

        @Test
        @DisplayName("should respect page size")
        void shouldRespectPageSize() {
            // when
            Page<User> page = repository.findAll(PageRequest.of(0, 1));

            // then
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("should return second page")
        void shouldReturnSecondPage() {
            // given

            // when
            Page<User> page = repository.findAll(PageRequest.of(1, 1));

            // then
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getNumber()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("should reject duplicate username")
        void shouldRejectDuplicateUsername() {
            // given
            User duplicate = new User();
            duplicate.setUsername("user");
            duplicate.setPassword("anotherPassword");

            // when & then
            assertThatThrownBy(() -> {
                repository.save(duplicate);
                em.flush();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}
