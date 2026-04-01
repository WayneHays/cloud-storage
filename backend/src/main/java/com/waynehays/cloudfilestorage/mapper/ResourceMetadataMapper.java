package com.waynehays.cloudfilestorage.mapper;

import com.waynehays.cloudfilestorage.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ResourceMetadataMapper {

    ResourceMetadataDto toDto(ResourceMetadata metadata);

    List<ResourceMetadataDto> toDto(List<ResourceMetadata> entities);
}
