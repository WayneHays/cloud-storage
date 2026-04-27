package com.waynehays.cloudfilestorage.files.operation.info;

import com.waynehays.cloudfilestorage.files.dto.response.ResourceDto;

public interface ResourceInfoServiceApi {

    ResourceDto getInfo(Long userId, String path);
}
