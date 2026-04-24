package com.waynehays.cloudfilestorage.resource.service.info;

import com.waynehays.cloudfilestorage.resource.dto.response.ResourceDto;

public interface ResourceInfoServiceApi {

    ResourceDto getInfo(Long userId, String path);
}
