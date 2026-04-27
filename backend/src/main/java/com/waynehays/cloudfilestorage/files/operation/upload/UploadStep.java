package com.waynehays.cloudfilestorage.files.operation.upload;

interface UploadStep {

    void execute(UploadContext context);

    default void rollback(UploadRollbackDto snapshot) {}

    default boolean requiresRollback(UploadRollbackDto snapshot) {
        return false;
    }
}
