package com.waynehays.cloudfilestorage.service.resource.move;

interface MoveStep {

    void execute(MoveContext context);

    default void rollback(MoveRollbackSnapshot snapshot) {}
}
