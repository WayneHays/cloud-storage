package com.waynehays.cloudfilestorage.component.converter;

import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.entity.ResourceMetadata;

public interface ResourceDtoConverterApi {

    ResourceDto fromMetadata(ResourceMetadata metaData);

    ResourceDto fileFromPath(String path, Long size);

    ResourceDto directoryFromPath(String path);
}
