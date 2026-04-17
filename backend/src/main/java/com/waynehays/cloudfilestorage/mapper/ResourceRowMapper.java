package com.waynehays.cloudfilestorage.mapper;

import com.waynehays.cloudfilestorage.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.dto.internal.metadata.DirectoryRowDto;
import com.waynehays.cloudfilestorage.dto.internal.metadata.FileRowDto;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface ResourceRowMapper {

    default FileRowDto toFileRow(UploadObjectDto uploadObject) {
        String path = uploadObject.fullPath();
        return new FileRowDto(
                path,
                PathUtils.normalizePath(PathUtils.extractParentPath(path)),
                PathUtils.extractFilename(path),
                uploadObject.size()
        );
    }

    default DirectoryRowDto toDirectoryRow(String path) {
        return new DirectoryRowDto(
                path,
                PathUtils.normalizePath(PathUtils.extractParentPath(path)),
                PathUtils.extractFilename(path)
        );
    }

    List<FileRowDto> toFileRows(List<UploadObjectDto> uploadObjects);

    List<DirectoryRowDto> toDirectoryRows(Set<String> paths);
}
