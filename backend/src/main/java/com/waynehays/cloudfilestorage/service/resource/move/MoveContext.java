package com.waynehays.cloudfilestorage.service.resource.move;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class MoveContext {
    private final List<MovedObject> movedObjects = Collections.synchronizedList(new ArrayList<>());

    void addMovedObject(String pathFrom, String pathTo) {
        movedObjects.add(new MovedObject(pathFrom, pathTo));
    }

    boolean doesNotContainMovedObjects() {
        return movedObjects.isEmpty();
    }

    record MovedObject(String pathFrom, String pathTo){
    }
}
