package com.waynehays.cloudfilestorage.mapper;

import com.waynehays.cloudfilestorage.dto.fileinfo.FileInfoDto;
import com.waynehays.cloudfilestorage.entity.FileInfo;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FileInfoMapper {
    FileInfoDto toDto(FileInfo fileInfo);

    List<FileInfoDto> toDtoList(List<FileInfo> fileInfoList);
}
