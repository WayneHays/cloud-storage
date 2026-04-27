package com.waynehays.cloudfilestorage.files.operation.move;

interface MoveStep {

    void execute(MoveContext context);

    default void rollback(MoveRollbackDto snapshot) {}

    default boolean requiresRollback(MoveRollbackDto snapshot) {
        return false;
    }
}
