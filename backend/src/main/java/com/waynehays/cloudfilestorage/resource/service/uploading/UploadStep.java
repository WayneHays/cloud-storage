package com.waynehays.cloudfilestorage.resource.service.uploading;

interface UploadStep {

    void execute(UploadContext context);

    default void rollback(UploadRollbackSnapshot snapshot) {}

    default boolean requiresRollback(UploadRollbackSnapshot snapshot) {
        return false;
    }
}
