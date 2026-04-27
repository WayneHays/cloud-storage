package com.waynehays.cloudfilestorage.files.operation.move;

import java.util.List;

record MoveRollbackDto(
        Long userId,
        List<MovedObject> movedObjects
) {
    boolean hasMovedObjects() {
        return !movedObjects.isEmpty();
    }
}
