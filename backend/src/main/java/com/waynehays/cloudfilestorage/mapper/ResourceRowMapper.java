package com.waynehays.cloudfilestorage.mapper;

import com.waynehays.cloudfilestorage.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.dto.internal.metadata.DirectoryRow;
import com.waynehays.cloudfilestorage.dto.internal.metadata.FileRow;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface ResourceRowMapper {

    default FileRow toFileRow(UploadObjectDto uploadObject) {
        String path = uploadObject.fullPath();
        return new FileRow(
                path,
                PathUtils.extractParentPath(path),
                PathUtils.extractFilename(path),
                uploadObject.size()
        );
    }

    default DirectoryRow toDirectoryRow(String path) {
        return new DirectoryRow(
                path,
                PathUtils.extractParentPath(path),
                PathUtils.extractFilename(path)
        );
    }

    List<FileRow> toFileRows(List<UploadObjectDto> uploadObjects);

    List<DirectoryRow> toDirectoryRows(Set<String> paths);
}
