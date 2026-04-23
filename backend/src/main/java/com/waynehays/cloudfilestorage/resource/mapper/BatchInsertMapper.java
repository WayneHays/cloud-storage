package com.waynehays.cloudfilestorage.resource.mapper;

import com.waynehays.cloudfilestorage.resource.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.resource.dto.internal.DirectoryRowDto;
import com.waynehays.cloudfilestorage.resource.dto.internal.FileRowDto;
import com.waynehays.cloudfilestorage.shared.utils.PathUtils;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface BatchInsertMapper {

    default FileRowDto toFileRow(UploadObjectDto uploadObject) {
        String path = uploadObject.fullPath();
        return new FileRowDto(
                path,
                PathUtils.normalizePath(path),
                PathUtils.normalizePath(PathUtils.extractParentPath(path)),
                PathUtils.extractFilename(path),
                uploadObject.size()
        );
    }

    default DirectoryRowDto toDirectoryRow(String path) {
        return new DirectoryRowDto(
                path,
                PathUtils.normalizePath(path),
                PathUtils.normalizePath(PathUtils.extractParentPath(path)),
                PathUtils.extractFilename(path)
        );
    }

    List<FileRowDto> toFileRows(List<UploadObjectDto> uploadObjects);

    List<DirectoryRowDto> toDirectoryRows(Set<String> paths);
}
