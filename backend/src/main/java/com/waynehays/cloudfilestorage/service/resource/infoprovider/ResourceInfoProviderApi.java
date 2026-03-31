package com.waynehays.cloudfilestorage.service.resource.infoprovider;

import com.waynehays.cloudfilestorage.dto.response.ResourceDto;

public interface ResourceInfoProviderApi {

    ResourceDto getInfo(Long userId, String path);
}
