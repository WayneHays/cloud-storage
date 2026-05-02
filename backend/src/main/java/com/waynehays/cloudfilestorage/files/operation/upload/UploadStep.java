package com.waynehays.cloudfilestorage.files.operation.upload;

interface UploadStep {

    void execute(Context context);

    default void rollback(RollbackDto snapshot) {}

    default boolean requiresRollback(RollbackDto snapshot) {
        return false;
    }
}
