package com.waynehays.cloudfilestorage.mapper;

import com.waynehays.cloudfilestorage.dto.internal.metadata.NewDirectoryDto;
import com.waynehays.cloudfilestorage.dto.internal.metadata.NewFileDto;
import com.waynehays.cloudfilestorage.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface NewResourceMapper {

    default NewFileDto toNewFile(UploadObjectDto uploadObject) {
        String path = uploadObject.fullPath();
        return new NewFileDto(
                path,
                PathUtils.extractParentPath(path),
                PathUtils.extractFilename(path),
                uploadObject.size()
        );
    }

    default NewDirectoryDto toNewDirectory(String path) {
        return new NewDirectoryDto(
                path,
                PathUtils.extractParentPath(path),
                PathUtils.extractFilename(path)
        );
    }

    List<NewFileDto> toNewFiles(List<UploadObjectDto> uploadObjects);

    List<NewDirectoryDto> toNewDirectories(Set<String> paths);
}
