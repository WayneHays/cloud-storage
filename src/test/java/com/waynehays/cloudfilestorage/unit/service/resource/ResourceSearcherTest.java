package com.waynehays.cloudfilestorage.unit.service.resource;

import com.waynehays.cloudfilestorage.component.converter.ResourceDtoConverterApi;
import com.waynehays.cloudfilestorage.component.keyresolver.StorageKeyResolverApi;
import com.waynehays.cloudfilestorage.dto.ResourceType;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.filestorage.dto.MetaData;
import com.waynehays.cloudfilestorage.filestorage.minio.MinioFileStorage;
import com.waynehays.cloudfilestorage.service.resource.searcher.ResourceSearcher;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceSearcherTest {
    private static final Long USER_ID = 1L;

    @Mock
    private MinioFileStorage fileStorage;

    @Mock
    private StorageKeyResolverApi keyResolver;

    @Mock
    private ResourceDtoConverterApi dtoConverter;

    @InjectMocks
    private ResourceSearcher resourceSearcher;

    @Nested
    class Search {

        @Test
        void shouldReturnMatchingResources() {
            // given
            String query = "file";
            String rootPrefix = "user-1-files/";
            String matchingKey = "user-1-files/directory/file.txt";
            MetaData matchingMetaData = new MetaData(matchingKey, "file.txt", 100L, "text/plain", true);
            ResourceDto expectedDto = new ResourceDto("folder/", "file.txt", 100L, ResourceType.FILE);

            when(keyResolver.resolveKeyToRoot(USER_ID)).thenReturn(rootPrefix);
            when(fileStorage.getListRecursive(rootPrefix)).thenReturn(List.of(matchingMetaData));
            when(keyResolver.extractPath(USER_ID, matchingKey)).thenReturn("directory/file.txt");
            when(dtoConverter.convert(matchingMetaData, "directory/file.txt")).thenReturn(expectedDto);

            // when
            List<ResourceDto> result = resourceSearcher.search(USER_ID, query);

            // then
            assertThat(result).containsExactly(expectedDto);
        }

        @Test
        void shouldFilterOutNonMatchingResources() {
            // given
            String query = "report";
            String rootPrefix = "user-1-files/";
            MetaData matching = new MetaData("user-1-files/report.pdf", "report.pdf", 200L, "text/plain", false);
            MetaData nonMatching = new MetaData("user-1-files/photo.png", "photo.png", 300L, "text/plain", true);
            ResourceDto expectedDto = new ResourceDto("", "report.pdf", 200L, ResourceType.FILE);

            when(keyResolver.resolveKeyToRoot(USER_ID)).thenReturn(rootPrefix);
            when(fileStorage.getListRecursive(rootPrefix)).thenReturn(List.of(matching, nonMatching));
            when(keyResolver.extractPath(USER_ID, matching.key())).thenReturn("report.pdf");
            when(dtoConverter.convert(matching, "report.pdf")).thenReturn(expectedDto);

            // when
            List<ResourceDto> result = resourceSearcher.search(USER_ID, query);

            // then
            assertThat(result).containsExactly(expectedDto);
            verify(keyResolver, never()).extractPath(USER_ID, nonMatching.key());
        }

        @Test
        void shouldMatchCaseInsensitively() {
            // given
            String query = "FILE";
            String rootPrefix = "user-1-files/";
            MetaData metaData = new MetaData("user-1-files/file.txt", "file.txt", 100L, "text/plain", true);
            ResourceDto expectedDto = new ResourceDto("", "file.txt", 100L, ResourceType.FILE);

            when(keyResolver.resolveKeyToRoot(USER_ID)).thenReturn(rootPrefix);
            when(fileStorage.getListRecursive(rootPrefix)).thenReturn(List.of(metaData));
            when(keyResolver.extractPath(USER_ID, metaData.key())).thenReturn("file.txt");
            when(dtoConverter.convert(metaData, "file.txt")).thenReturn(expectedDto);

            // when
            List<ResourceDto> result = resourceSearcher.search(USER_ID, query);

            // then
            assertThat(result).containsExactly(expectedDto);
        }

        @Test
        void shouldReturnEmptyListWhenNothingMatches() {
            // given
            String query = "nonexistent";
            String rootPrefix = "user-1-files/";
            MetaData metaData = new MetaData("user-1-files/file.txt", "file.txt", 100L, "text/plain", false);

            when(keyResolver.resolveKeyToRoot(USER_ID)).thenReturn(rootPrefix);
            when(fileStorage.getListRecursive(rootPrefix)).thenReturn(List.of(metaData));

            // when
            List<ResourceDto> result = resourceSearcher.search(USER_ID, query);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyListWhenStorageIsEmpty() {
            // given
            String query = "file";
            String rootPrefix = "user-1-files/";

            when(keyResolver.resolveKeyToRoot(USER_ID)).thenReturn(rootPrefix);
            when(fileStorage.getListRecursive(rootPrefix)).thenReturn(List.of());

            // when
            List<ResourceDto> result = resourceSearcher.search(USER_ID, query);

            // then
            assertThat(result).isEmpty();
        }
    }
}
