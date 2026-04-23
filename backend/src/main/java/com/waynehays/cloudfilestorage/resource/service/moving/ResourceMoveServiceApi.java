package com.waynehays.cloudfilestorage.resource.service.moving;

import com.waynehays.cloudfilestorage.resource.dto.response.ResourceDto;

public interface ResourceMoveServiceApi {

    ResourceDto move(Long userId, String pathFrom, String pathTo);
}
