package com.waynehays.cloudfilestorage.core.metadata.mapper;

import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.core.metadata.entity.ResourceMetadata;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ResourceMetadataMapper {

    ResourceMetadataDto toDto(ResourceMetadata entity);

    List<ResourceMetadataDto> toDto(List<ResourceMetadata> entities);
}
