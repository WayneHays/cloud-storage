package com.waynehays.cloudfilestorage.service.resource.move;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
class MoveContext {
    private final List<MovedObject> movedObjects = Collections.synchronizedList(new ArrayList<>());

    void addMovedObject(String pathFrom, String pathTo) {
        movedObjects.add(new MovedObject(pathFrom, pathTo));
    }

    List<MovedObject> getMovedObjects() {
        return List.copyOf(movedObjects);
    }

    record MovedObject(String pathFrom, String pathTo){
    }
}
