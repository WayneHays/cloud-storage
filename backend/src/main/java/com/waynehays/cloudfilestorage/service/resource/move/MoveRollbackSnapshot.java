package com.waynehays.cloudfilestorage.service.resource.move;

import java.util.List;

record MoveRollbackSnapshot(
        Long userId,
        List<MovedObject> movedObjects
) {
    boolean hasMovedObjects() {
        return !movedObjects.isEmpty();
    }
}
