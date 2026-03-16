package com.waynehays.cloudfilestorage.component.converter;

import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.filestorage.dto.MetaData;

public interface ResourceDtoConverterApi {

    ResourceDto convert (MetaData metaData, String path);

    ResourceDto fileFromPath(String path, Long size);

    ResourceDto directoryFromPath(String path);
}
