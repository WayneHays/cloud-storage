package com.waynehays.cloudfilestorage.files.operation.upload.step;

import com.waynehays.cloudfilestorage.files.operation.upload.dto.Context;
import com.waynehays.cloudfilestorage.files.operation.upload.dto.RollbackDto;

public interface UploadStep {

    void execute(Context context);

    default void rollback(RollbackDto rollbackDto) {}

    default boolean requiresRollback(RollbackDto rollbackDto) {
        return false;
    }
}
