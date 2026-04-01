package com.waynehays.cloudfilestorage.unit.service.metadata;

import com.waynehays.cloudfilestorage.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.mapper.ResourceMetadataMapper;
import com.waynehays.cloudfilestorage.repository.ResourceMetadataRepository;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.service.storagequota.UsedSpace;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
    private ResourceMetadataServiceApi service;

    private static final Long USER_ID = 1L;

    @Nested
    class FindOrThrow {

        @Test
        void shouldReturnDtoWhenFound() {
            // given
            String path = "docs/file.txt";
            ResourceMetadata metadata = createFileMetadata(path, 100L);
            ResourceMetadataDto dto = createFileDto(path, 100L);

            when(repository.findByUserIdAndPathAndMarkedForDeletionFalse(USER_ID, path))
                    .thenReturn(Optional.of(metadata));
            when(mapper.toDto(metadata)).thenReturn(dto);

            // when
            ResourceMetadataDto result = service.findOrThrow(USER_ID, path);

            // then
            assertThat(result).isEqualTo(dto);
        }

        @Test
        void shouldThrowWhenNotFound() {
            // given
            String path = "docs/file.txt";

            when(repository.findByUserIdAndPathAndMarkedForDeletionFalse(USER_ID, path))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.findOrThrow(USER_ID, path))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class FindDirectChildren {

        @Test
        void shouldReturnChildrenForNonEmptyPath() {
            // given
            String directoryPath = "docs/";
            ResourceMetadata metadata = createFileMetadata("docs/file.txt", 100L);
            ResourceMetadataDto dto = createFileDto("docs/file.txt", 100L);

            when(repository.findByUserIdAndPathAndMarkedForDeletionFalse(USER_ID, directoryPath))
                    .thenReturn(Optional.of(createDirectoryMetadata(directoryPath)));
            when(repository.findByUserIdAndParentPathAndMarkedForDeletionFalse(USER_ID, directoryPath))
                    .thenReturn(List.of(metadata));
            when(mapper.toDto(List.of(metadata))).thenReturn(List.of(dto));

            // when
            List<ResourceMetadataDto> result = service.findDirectChildren(USER_ID, directoryPath);

            // then
            assertThat(result).containsExactly(dto);
        }

        @Test
        void shouldSkipExistenceCheckForEmptyPath() {
            // given
            String rootPath = "";
            ResourceMetadata metadata = createDirectoryMetadata("docs/");
            ResourceMetadataDto dto = createDirectoryDto();

            when(repository.findByUserIdAndParentPathAndMarkedForDeletionFalse(USER_ID, rootPath))
                    .thenReturn(List.of(metadata));
            when(mapper.toDto(List.of(metadata))).thenReturn(List.of(dto));

            // when
            List<ResourceMetadataDto> result = service.findDirectChildren(USER_ID, rootPath);

            // then
            assertThat(result).containsExactly(dto);
            verify(repository, never()).findByUserIdAndPathAndMarkedForDeletionFalse(
                    eq(USER_ID),
                    anyString());
        }

        @Test
        void shouldThrowWhenDirectoryNotFound() {
            // given
            String directoryPath = "nonexistent/";

            when(repository.findByUserIdAndPathAndMarkedForDeletionFalse(USER_ID, directoryPath))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.findDirectChildren(USER_ID, directoryPath))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class FindDirectoryContent {

        @Test
        void shouldReturnContentByPrefix() {
            // given
            String prefix = "docs/";
            ResourceMetadata file = createFileMetadata("docs/file.txt", 100L);
            ResourceMetadataDto fileDto = createFileDto("docs/file.txt", 100L);

            when(repository.findByUserIdAndPathStartingWithAndMarkedForDeletionFalse(USER_ID, prefix))
                    .thenReturn(List.of(file));
            when(mapper.toDto(List.of(file))).thenReturn(List.of(fileDto));

            // when
            List<ResourceMetadataDto> result = service.findDirectoryContent(USER_ID, prefix);

            // then
            assertThat(result).containsExactly(fileDto);
        }
    }

    @Nested
    class FindByNameContaining {

        @Test
        void shouldReturnMatchingResources() {
            // given
            String query = "file";
            ResourceMetadata metadata = createFileMetadata("docs/file.txt", 100L);
            ResourceMetadataDto dto = createFileDto("docs/file.txt", 100L);

            when(repository.findByUserIdAndNameContainingIgnoreCaseAndMarkedForDeletionFalse(USER_ID, query))
                    .thenReturn(List.of(metadata));
            when(mapper.toDto(List.of(metadata))).thenReturn(List.of(dto));

            // when
            List<ResourceMetadataDto> result = service.findByNameContaining(USER_ID, query);

            // then
            assertThat(result).containsExactly(dto);
        }
    }

    @Nested
    class FindMarkedForDeletion {

        @Test
        void shouldReturnMarkedOrphans() {
            // given
            ResourceMetadata orphan = createFileMetadata("docs/orphan.txt", 50L);
            ResourceMetadataDto orphanDto = createFileDto("docs/orphan.txt", 50L);

            when(repository.findByMarkedForDeletionTrue()).thenReturn(List.of(orphan));
            when(mapper.toDto(List.of(orphan))).thenReturn(List.of(orphanDto));

            // when
            List<ResourceMetadataDto> result = service.findMarkedForDeletion();

            // then
            assertThat(result).containsExactly(orphanDto);
        }
    }

    @Nested
    class FindExistingPaths {

        @Test
        void shouldReturnExistingPaths() {
            // given
            Set<String> paths = Set.of("docs/file.txt", "docs/other.txt");
            Set<String> existing = Set.of("docs/file.txt");

            when(repository.findExistingPaths(USER_ID, paths)).thenReturn(existing);

            // when
            Set<String> result = service.findExistingPaths(USER_ID, paths);

            // then
            assertThat(result).containsExactly("docs/file.txt");
        }
    }

    @Nested
    class GetUsedSpaceOfUsers {

        @Test
        void shouldReturnUsedSpaceForUsers() {
            // given
            List<Long> userIds = List.of(1L, 2L);
            List<UsedSpace> usedSpaces = List.of(
                    mock(UsedSpace.class),
                    mock(UsedSpace.class)
            );

            when(repository.sumSizeGroupByUserId(userIds, ResourceType.FILE)).thenReturn(usedSpaces);

            // when
            List<UsedSpace> result = service.getUsedSpaceOfUsers(userIds);

            // then
            assertThat(result).isEqualTo(usedSpaces);
        }
    }

    @Nested
    class SumResourceSizesByPrefix {

        @Test
        void shouldReturnSumOfSizes() {
            // given
            String prefix = "docs/";

            when(repository.sumSizeByPrefix(USER_ID, prefix, ResourceType.FILE)).thenReturn(500L);

            // when
            long result = service.sumResourceSizesByPrefix(USER_ID, prefix);

            // then
            assertThat(result).isEqualTo(500L);
        }
    }

    @Nested
    class ValidateDirectoryCreation {

        @Test
        void shouldPassWhenDirectoryDoesNotExistAndParentExists() {
            // given
            String path = "docs/newdir/";
            Set<String> existing = Set.of("docs/");

            when(repository.findExistingPaths(eq(USER_ID), anySet())).thenReturn(existing);

            // when & then
            service.validateDirectoryCreation(USER_ID, path);
        }

        @Test
        void shouldThrowWhenDirectoryAlreadyExists() {
            // given
            String path = "docs/existing/";
            Set<String> existing = Set.of("docs/existing/", "docs/");

            when(repository.findExistingPaths(eq(USER_ID), anySet())).thenReturn(existing);

            // when & then
            assertThatThrownBy(() -> service.validateDirectoryCreation(USER_ID, path))
                    .isInstanceOf(ResourceAlreadyExistsException.class);
        }

        @Test
        void shouldThrowWhenParentDoesNotExist() {
            // given
            String path = "nonexistent/newdir/";
            Set<String> existing = Set.of();

            when(repository.findExistingPaths(eq(USER_ID), anySet())).thenReturn(existing);

            // when & then
            assertThatThrownBy(() -> service.validateDirectoryCreation(USER_ID, path))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void shouldNotCheckParentForRootLevelDirectory() {
            // given
            String path = "rootdir/";
            Set<String> existing = Set.of();

            when(repository.findExistingPaths(eq(USER_ID), anySet())).thenReturn(existing);

            // when & then
            service.validateDirectoryCreation(USER_ID, path);
        }
    }

    @Nested
    class ThrowIfAnyExists {

        @Test
        void shouldPassWhenNoneExist() {
            // given
            List<String> paths = List.of("docs/new1.txt", "docs/new2.txt");

            when(repository.findExistingPaths(eq(USER_ID), anySet())).thenReturn(Set.of());

            // when & then
            assertThatThrownBy(() -> service.throwIfAnyExists(USER_ID, paths))
                    .isInstanceOf(ResourceAlreadyExistsException.class);
        }

        @Test
        void shouldThrowWhenAnyExist() {
            // given
            List<String> paths = List.of("docs/existing.txt", "docs/new.txt");
            Set<String> existing = Set.of("docs/existing.txt");

            when(repository.findExistingPaths(eq(USER_ID), anySet())).thenReturn(existing);

            // when & then
            assertThatThrownBy(() -> service.throwIfAnyExists(USER_ID, paths))
                    .isInstanceOf(ResourceAlreadyExistsException.class);
        }
    }

    @Nested
    class SaveFile {

        @Test
        void shouldSaveFileMetadata() {
            // given
            String path = "docs/file.txt";
            long size = 1024L;

            // when
            service.saveFile(USER_ID, path, size);

            // then
            ArgumentCaptor<ResourceMetadata> captor = ArgumentCaptor.forClass(ResourceMetadata.class);
            verify(repository).save(captor.capture());

            ResourceMetadata saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getPath()).isEqualTo(path);
            assertThat(saved.getParentPath()).isEqualTo("docs/");
            assertThat(saved.getName()).isEqualTo("file.txt");
            assertThat(saved.getSize()).isEqualTo(size);
            assertThat(saved.getType()).isEqualTo(ResourceType.FILE);
            assertThat(saved.isMarkedForDeletion()).isFalse();
        }

        @Test
        void shouldThrowWhenPathEndsWithSlash() {
            // given
            String path = "docs/file/";

            // when & then
            assertThatThrownBy(() -> service.saveFile(USER_ID, path, 100L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class SaveDirectories {

        @Test
        void shouldSaveAllDirectories() {
            // given
            Set<String> paths = Set.of("docs", "images");

            // when
            service.saveDirectories(USER_ID, paths);

            // then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<ResourceMetadata>> captor = ArgumentCaptor.forClass(List.class);
            verify(repository).saveAll(captor.capture());

            List<ResourceMetadata> saved = captor.getValue();
            assertThat(saved).hasSize(2)
                    .allMatch(m -> m.getType() == ResourceType.DIRECTORY)
                    .allMatch(m -> m.getUserId().equals(USER_ID))
                    .allMatch(m -> m.getPath().endsWith("/"));
        }
    }

    @Nested
    class UpdatePathsByPrefix {

        @Test
        void shouldDelegateToRepository() {
            // given
            String prefixFrom = "docs/";
            String prefixTo = "archive/";

            // when
            service.updatePathsByPrefix(USER_ID, prefixFrom, prefixTo);

            // then
            verify(repository).updatePathsByPrefix(USER_ID, prefixFrom, prefixTo);
        }
    }

    @Nested
    class MarkForDeletion {

        @Test
        void shouldDelegateToRepository() {
            // given
            String path = "docs/file.txt";

            // when
            service.markForDeletion(USER_ID, path);

            // then
            verify(repository).markForDeletion(USER_ID, path);
        }
    }

    @Nested
    class MarkForDeletionByPrefix {

        @Test
        void shouldDelegateToRepository() {
            // given
            String prefix = "docs/";

            // when
            service.markForDeletionByPrefix(USER_ID, prefix);

            // then
            verify(repository).markForDeletionByPrefix(USER_ID, prefix);
        }
    }

    @Nested
    class Delete {

        @Test
        void shouldDelegateToRepository() {
            // given
            String path = "docs/file.txt";

            // when
            service.delete(USER_ID, path);

            // then
            verify(repository).deleteByUserIdAndPath(USER_ID, path);
        }
    }

    @Nested
    class DeleteByPrefix {

        @Test
        void shouldDelegateToRepository() {
            // given
            String prefix = "docs/";

            // when
            service.deleteByPrefix(USER_ID, prefix);

            // then
            verify(repository).deleteByUserIdAndPathStartingWith(USER_ID, prefix);
        }
    }

    @Nested
    class DeleteById {

        @Test
        void shouldDelegateToRepository() {
            // given
            Long id = 42L;

            // when
            service.deleteById(id);

            // then
            verify(repository).deleteById(id);
        }
    }

    private ResourceMetadata createFileMetadata(String path, Long size) {
        ResourceMetadata metadata = new ResourceMetadata();
        metadata.setId(1L);
        metadata.setUserId(USER_ID);
        metadata.setPath(path);
        metadata.setParentPath(path.substring(0, path.lastIndexOf('/') + 1));
        metadata.setName(path.substring(path.lastIndexOf('/') + 1));
        metadata.setSize(size);
        metadata.setType(ResourceType.FILE);
        metadata.setMarkedForDeletion(false);
        return metadata;
    }

    private ResourceMetadata createDirectoryMetadata(String path) {
        ResourceMetadata metadata = new ResourceMetadata();
        metadata.setId(2L);
        metadata.setUserId(USER_ID);
        metadata.setPath(path);
        int index = path.lastIndexOf('/', path.length() - 2) + 1;
        metadata.setParentPath(path.substring(0, index));
        metadata.setName(path.substring(index, path.length() - 1));
        metadata.setSize(null);
        metadata.setType(ResourceType.DIRECTORY);
        metadata.setMarkedForDeletion(false);

        return metadata;
    }

    private ResourceMetadataDto createFileDto(String path, Long size) {
        String parentPath = path.substring(0, path.lastIndexOf('/') + 1);
        String name = path.substring(path.lastIndexOf('/') + 1);
        return new ResourceMetadataDto(1L, USER_ID, path, parentPath, name, size, ResourceType.FILE);
    }

    private ResourceMetadataDto createDirectoryDto() {
        String parentPath = "docs/".substring(0, 0);
        String name = "docs/".substring(0, "docs/".length() - 1);
        return new ResourceMetadataDto(2L, USER_ID, "docs/", parentPath, name, null, ResourceType.DIRECTORY);
    }
}