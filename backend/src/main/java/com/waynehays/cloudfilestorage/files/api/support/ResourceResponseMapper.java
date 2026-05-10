package com.waynehays.cloudfilestorage.files.api.support;

import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.files.api.dto.response.ResourceResponse;
import com.waynehays.cloudfilestorage.files.operation.upload.dto.UploadObjectDto;
import com.waynehays.cloudfilestorage.utils.PathUtils;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ResourceResponseMapper {

    @IterableMapping(qualifiedByName = "withDisplayName")
    List<ResourceResponse> fromResourceMetadataDto(List<ResourceMetadataDto> dtos);

    @Named("withDisplayName")
    @Mapping(source = "parentPath", target = "path")
    @Mapping(source = "dto", target = "name", qualifiedByName = "displayName")
    ResourceResponse fromResourceMetadataDto(ResourceMetadataDto dto);

    @Mapping(source = "parentPath", target = "path")
    ResourceResponse toCreatedDirectoryResponse(ResourceMetadataDto dto);

    @Mapping(source = "fullPath", target = "path", qualifiedByName = "extractParentPath")
    @Mapping(source = "filename", target = "name")
    @Mapping(target = "type", constant = "FILE")
    ResourceResponse fromUploadObjectDto(UploadObjectDto dto);

    @Named("displayName")
    default String displayName(ResourceMetadataDto dto) {
        return dto.isFile()
                ? dto.name()
                : PathUtils.ensureTrailingSlash(dto.name());
    }

    @Named("extractParentPath")
    default String extractParentPath(String path) {
        return PathUtils.getParentPath(path);
    }
}
