package com.waynehays.cloudfilestorage.service.resource.upload;

interface UploadStep {

    void execute(UploadContext context);

    default void rollback(RollbackSnapshot snapshot) {}
}
