package com.waynehays.cloudfilestorage.mapper;

import com.waynehays.cloudfilestorage.dto.internal.NewFileDto;
import com.waynehays.cloudfilestorage.dto.internal.UploadObjectDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NewFileMapper {

    @Mapping(target = "path", source = "fullPath")
    NewFileDto toNewFile(UploadObjectDto uploadObject);

    List<NewFileDto> toNewFiles(List<UploadObjectDto> uploadObjects);
}
