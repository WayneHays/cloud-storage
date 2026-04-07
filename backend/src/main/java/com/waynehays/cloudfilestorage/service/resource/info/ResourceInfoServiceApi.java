package com.waynehays.cloudfilestorage.service.resource.info;

import com.waynehays.cloudfilestorage.dto.response.ResourceDto;

public interface ResourceInfoServiceApi {

    ResourceDto getInfo(Long userId, String path);
}
