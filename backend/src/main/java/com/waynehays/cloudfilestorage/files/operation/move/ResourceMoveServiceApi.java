package com.waynehays.cloudfilestorage.files.operation.move;

import com.waynehays.cloudfilestorage.files.dto.response.ResourceDto;

public interface ResourceMoveServiceApi {

    ResourceDto move(Long userId, String pathFrom, String pathTo);
}
