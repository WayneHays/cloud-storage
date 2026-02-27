package com.waynehays.cloudfilestorage.unit.mapper;

import com.waynehays.cloudfilestorage.dto.file.response.ResourceDto;
import com.waynehays.cloudfilestorage.dto.file.response.ResourceType;
import com.waynehays.cloudfilestorage.dto.fileinfo.FileInfoDto;
import com.waynehays.cloudfilestorage.mapper.ResourceMapper;
import com.waynehays.cloudfilestorage.mapper.ResourceMapperImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceMapperTest {

    private final ResourceMapper mapper = new ResourceMapperImpl();

    @Test
    @DisplayName("Should map FileInfoDto to ResourceDto with FILE type")
    void shouldMapToResourceDto() {
        FileInfoDto dto = new FileInfoDto(1L, "documents/work", "report.pdf",
                "user-1-files/documents/work/report.pdf", "application/pdf", 1024L);

        ResourceDto result = mapper.toDto(dto);

        assertThat(result.path()).isEqualTo("documents/work");
        assertThat(result.name()).isEqualTo("report.pdf");
        assertThat(result.size()).isEqualTo(1024L);
        assertThat(result.type()).isEqualTo(ResourceType.FILE);
    }

    @Test
    @DisplayName("Should map list of FileInfoDto to list of ResourceDto")
    void shouldMapList() {
        FileInfoDto dto1 = new FileInfoDto(1L, "docs", "file1.txt", "key1", "text/plain", 100L);
        FileInfoDto dto2 = new FileInfoDto(2L, "docs", "file2.txt", "key2", "text/plain", 200L);

        List<ResourceDto> result = mapper.toDtoList(List.of(dto1, dto2));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).type()).isEqualTo(ResourceType.FILE);
        assertThat(result.get(1).type()).isEqualTo(ResourceType.FILE);
    }
}
