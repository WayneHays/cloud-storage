package com.waynehays.cloudfilestorage.resource.service.moving;

import java.util.List;

record MoveRollbackSnapshot(
        Long userId,
        List<MovedObject> movedObjects
) {
    boolean hasMovedObjects() {
        return !movedObjects.isEmpty();
    }
}
