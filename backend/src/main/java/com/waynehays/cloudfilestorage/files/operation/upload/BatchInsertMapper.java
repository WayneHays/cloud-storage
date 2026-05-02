package com.waynehays.cloudfilestorage.files.operation.upload;

import com.waynehays.cloudfilestorage.core.metadata.dto.DirectoryRowDto;
import com.waynehays.cloudfilestorage.core.metadata.dto.FileRowDto;
import com.waynehays.cloudfilestorage.core.utils.PathUtils;
import com.waynehays.cloudfilestorage.files.dto.internal.UploadObjectDto;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
interface BatchInsertMapper {

    default FileRowDto toFileRow(UploadObjectDto uploadObject) {
        String path = uploadObject.fullPath();
        return new FileRowDto(
                uploadObject.storageKey(),
                path,
                PathUtils.normalizePath(path),
                PathUtils.normalizePath(PathUtils.extractParentPath(path)),
                PathUtils.extractName(path),
                uploadObject.size()
        );
    }

    default DirectoryRowDto toDirectoryRow(String path) {
        return new DirectoryRowDto(
                path,
                PathUtils.normalizePath(path),
                PathUtils.normalizePath(PathUtils.extractParentPath(path)),
                PathUtils.extractName(path)
        );
    }

    List<FileRowDto> toFileRows(List<UploadObjectDto> uploadObjects);

    List<DirectoryRowDto> toDirectoryRows(Set<String> paths);
}
