package com.waynehays.cloudfilestorage.service.resource.move;

import com.waynehays.cloudfilestorage.dto.response.ResourceDto;

public interface ResourceMoveServiceApi {

    ResourceDto move(Long userId, String pathFrom, String pathTo);
}
