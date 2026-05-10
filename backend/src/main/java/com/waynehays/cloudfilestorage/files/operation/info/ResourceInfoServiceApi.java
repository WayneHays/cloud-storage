package com.waynehays.cloudfilestorage.files.operation.info;

import com.waynehays.cloudfilestorage.files.api.dto.response.ResourceResponse;

public interface ResourceInfoServiceApi {

    ResourceResponse getInfo(Long userId, String path);
}
