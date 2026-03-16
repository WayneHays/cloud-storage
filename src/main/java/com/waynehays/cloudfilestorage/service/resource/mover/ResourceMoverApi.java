package com.waynehays.cloudfilestorage.service.resource.mover;

import com.waynehays.cloudfilestorage.dto.response.ResourceDto;

public interface ResourceMoverApi {

    ResourceDto move(Long userId, String pathFrom, String pathTo);
}
