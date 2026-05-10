package com.waynehays.cloudfilestorage.files.operation.move;

import com.waynehays.cloudfilestorage.files.api.dto.response.ResourceResponse;

public interface ResourceMoveServiceApi {

    ResourceResponse move(Long userId, String pathFrom, String pathTo);
}
