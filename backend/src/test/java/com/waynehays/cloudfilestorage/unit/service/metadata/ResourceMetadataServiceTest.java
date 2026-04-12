package com.waynehays.cloudfilestorage.unit.service.metadata;

import com.waynehays.cloudfilestorage.dto.internal.metadata.DirectoryRow;
import com.waynehays.cloudfilestorage.dto.internal.metadata.FileRow;
import com.waynehays.cloudfilestorage.dto.internal.metadata.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.dto.internal.quota.UsedSpace;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.entity.ResourceType;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.mapper.ResourceMetadataMapper;
import com.waynehays.cloudfilestorage.repository.metadata.ResourceMetadataRepository;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;

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

    @Mock
    private ResourceMetadataMapper mapper;

    @Mock
    private ResourceMetadataRepository repository;

    @InjectMocks
    private ResourceMetadataService service;

    private static final Long USER_ID = 1L;

    @Nested
    class ReadOperations {

        @Test
        void findOrThrow_shouldReturnDtoWhenFound() {
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
        void findOrThrow_shouldThrowWhenNotFound() {
            // given
            when(repository.findByPath(USER_ID, "missing.txt"))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.findOrThrow(USER_ID, "missing.txt"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void findDirectoryContent_shouldCheckExistenceAndReturnContent() {
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
        void findDirectoryContent_shouldSkipExistenceCheckForRootPath() {
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

        @Test
        void findFilesByPathPrefix_shouldReturnMappedFiles() {
            // given
            ResourceMetadata entity = new ResourceMetadata();
            ResourceMetadataDto dto = new ResourceMetadataDto(
                    1L, USER_ID, "docs/file.txt", "docs/", "file.txt",
                    100L, ResourceType.FILE);

            when(repository.findFilesByPathPrefix(USER_ID, "docs/"))
                    .thenReturn(List.of(entity));
            when(mapper.toResourceMetadataDto(List.of(entity)))
                    .thenReturn(List.of(dto));

            // when
            List<ResourceMetadataDto> result = service.findFilesByPathPrefix(USER_ID, "docs/");

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        void findByNameContaining_shouldReturnMappedResults() {
            // given
            ResourceMetadata entity = new ResourceMetadata();
            ResourceMetadataDto dto = new ResourceMetadataDto(
                    1L, USER_ID, "docs/report.pdf", "docs/", "report.pdf",
                    200L, ResourceType.FILE);

            when(repository.findByNameContaining(USER_ID, "report", Pageable.ofSize(10)))
                    .thenReturn(List.of(entity));
            when(mapper.toResourceMetadataDto(List.of(entity)))
                    .thenReturn(List.of(dto));

            // when
            List<ResourceMetadataDto> result = service.findByNameContaining(USER_ID, "report", 10);

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        void findFilesMarkedForDeletion_shouldReturnMappedFiles() {
            // given
            ResourceMetadata entity = new ResourceMetadata();
            ResourceMetadataDto dto = new ResourceMetadataDto(
                    1L, USER_ID, "old.txt", "", "old.txt",
                    50L, ResourceType.FILE);

            when(repository.findFilesMarkedForDeletion(Pageable.ofSize(100)))
                    .thenReturn(List.of(entity));
            when(mapper.toResourceMetadataDto(List.of(entity)))
                    .thenReturn(List.of(dto));

            // when
            List<ResourceMetadataDto> result = service.findFilesMarkedForDeletion(100);

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        void findExistingPaths_shouldDelegateToRepository() {
            // given
            Set<String> paths = Set.of("docs/", "images/");
            when(repository.findExistingPaths(USER_ID, paths))
                    .thenReturn(Set.of("docs/"));

            // when
            Set<String> result = service.findExistingPaths(USER_ID, paths);

            // then
            assertThat(result).containsExactly("docs/");
        }

        @Test
        void getUsedSpaceByUsers_shouldDelegateToRepository() {
            // given
            List<Long> userIds = List.of(1L, 2L);
            when(repository.sumFileSizesGroupByUserId(userIds, ResourceType.FILE))
                    .thenReturn(List.of());

            // when
            List<UsedSpace> result = service.getUsedSpaceByUsers(userIds);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class WriteOperations {

        @Test
        void saveFiles_shouldConvertToParamsAndDelegate() {
            // given
            FileRow file = new FileRow("docs/file.txt", "docs/", "file.txt", 100L);

            // when
            service.saveFiles(USER_ID, List.of(file));

            // then
            verify(repository).saveFiles(USER_ID, List.of(file));
        }

        @Test
        void saveDirectories_shouldConvertToParamsAndDelegate() {
            // given
            DirectoryRow directory = new DirectoryRow("docs/reports/", "docs/", "reports");

            // when
            service.saveDirectories(USER_ID, List.of(directory));

            // then
            verify(repository).saveDirectories(USER_ID, List.of(directory));
        }

        @Test
        void saveDirectory_shouldMapAndSave() {
            // given
            ResourceMetadata entity = new ResourceMetadata();
            when(mapper.toDirectoryEntity(USER_ID, "docs/")).thenReturn(entity);

            // when
            service.saveDirectory(USER_ID, "docs/");

            // then
            verify(repository).saveAndFlush(entity);
        }

        @Test
        void saveDirectory_shouldThrowException() {
            // given
            ResourceMetadata directory = new ResourceMetadata();
            when(mapper.toDirectoryEntity(USER_ID, "docs/")).thenReturn(directory);
            when(repository.saveAndFlush(directory)).thenThrow(new DataIntegrityViolationException("Duplicate"));

            // when & then
            assertThatThrownBy(() -> service.saveDirectory(USER_ID, "docs/"))
                    .isInstanceOf(ResourceAlreadyExistsException.class);
        }

        @Test
        void markForDeletionAndSumFileSize_shouldDelegateToRepository() {
            // given
            when(repository.markForDeletionAndSumSize(USER_ID, "docs/"))
                    .thenReturn(500L);

            // when
            long result = service.markForDeletionAndSumFileSize(USER_ID, "docs/");

            // then
            assertThat(result).isEqualTo(500L);
        }

        @Test
        void updatePathsByPrefix_shouldDelegateToRepository() {
            // when
            service.updatePathsByPrefix(USER_ID, "old/", "new/");

            // then
            verify(repository).updatePathsByPathPrefix(USER_ID, "old/", "new/");
        }

        @Test
        void markForDeletion_shouldDelegateToRepository() {
            // when
            service.markForDeletion(USER_ID, "docs/file.txt");

            // then
            verify(repository).markForDeletionByPath(USER_ID, "docs/file.txt");
        }
    }

    @Nested
    class DeleteOperations {

        @Test
        void deleteByPath_shouldDelegateToRepository() {
            // when
            service.deleteByPath(USER_ID, "docs/file.txt");

            // then
            verify(repository).deleteByPath(USER_ID, "docs/file.txt");
        }

        @Test
        void deleteByPathPrefix_shouldDelegateToRepository() {
            // when
            service.deleteByPathPrefix(USER_ID, "docs/");

            // then
            verify(repository).deleteByPathPrefix(USER_ID, "docs/");
        }

        @Test
        void deleteByPaths_shouldDelegateToRepository() {
            // given
            List<String> paths = List.of("file1.txt", "file2.txt");

            // when
            service.deleteByPaths(USER_ID, paths);

            // then
            verify(repository).deleteByPaths(USER_ID, paths);
        }

        @Test
        void deleteAllByIds_shouldDelegateToRepository() {
            // given
            List<Long> ids = List.of(1L, 2L, 3L);

            // when
            service.deleteByIds(ids);

            // then
            verify(repository).deleteByIds(ids);
        }
    }
}
