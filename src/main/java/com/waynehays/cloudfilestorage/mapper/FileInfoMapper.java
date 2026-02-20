package com.waynehays.cloudfilestorage.mapper;

import com.waynehays.cloudfilestorage.dto.files.response.ResourceDto;
import com.waynehays.cloudfilestorage.entity.FileInfo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FileInfoMapper {

    default ResourceDto toResourceDto(FileInfo fileInfo) {
        return ResourceDto.file(
                fileInfo.getDirectory(),
                fileInfo.getName(),
                fileInfo.getSize()
        );
    }
}
