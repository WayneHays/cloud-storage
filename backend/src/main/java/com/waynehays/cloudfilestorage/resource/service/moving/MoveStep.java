package com.waynehays.cloudfilestorage.resource.service.moving;

interface MoveStep {

    void execute(MoveContext context);

    default void rollback(MoveRollbackSnapshot snapshot) {}

    default boolean requiresRollback(MoveRollbackSnapshot snapshot) {
        return false;
    }
}
