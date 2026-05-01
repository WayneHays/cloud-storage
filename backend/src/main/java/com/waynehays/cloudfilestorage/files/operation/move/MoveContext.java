package com.waynehays.cloudfilestorage.files.operation.move;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class MoveContext {

    @Getter
    private final Long userId;

    @Getter
    private final String pathFrom;

    @Getter
    private final String pathTo;

    private final List<MovedObject> movedObjects = Collections.synchronizedList(new ArrayList<>());

    MoveContext(Long userId, String pathFrom, String pathTo) {
        this.userId = userId;
        this.pathFrom = pathFrom;
        this.pathTo = pathTo;
    }

    void addMovedObject(String from, String to) {
        movedObjects.add(new MovedObject(from, to));
    }

    MoveRollbackDto rollbackSnapshot() {
        return new MoveRollbackDto(userId, List.copyOf(movedObjects));
    }
}
