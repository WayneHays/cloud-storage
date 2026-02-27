package com.waynehays.cloudfilestorage.mapper;

import com.waynehays.cloudfilestorage.dto.file.response.ResourceDto;
import com.waynehays.cloudfilestorage.dto.fileinfo.FileInfoDto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ResourceMapper {

    default ResourceDto toDto(FileInfoDto fileInfoDto) {
        return ResourceDto.file(
                fileInfoDto.directory(),
                fileInfoDto.name(),
                fileInfoDto.size()
        );
    }

    default List<ResourceDto> toDtoList(List<FileInfoDto> fileInfoDtos) {
        return fileInfoDtos.stream()
                .map(this::toDto)
                .toList();
    }
}
