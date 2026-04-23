package com.waynehays.cloudfilestorage.resource.service.moving;

import com.waynehays.cloudfilestorage.resource.dto.internal.ResourceMetadataDto;
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

    @Getter
    private final ResourceMetadataDto metadata;

    private final List<MovedObject> movedObjects = Collections.synchronizedList(new ArrayList<>());

    MoveContext(Long userId, String pathFrom, String pathTo, ResourceMetadataDto metadata) {
        this.userId = userId;
        this.pathFrom = pathFrom;
        this.pathTo = pathTo;
        this.metadata = metadata;
    }

    boolean isMovingFile() {
        return metadata.isFile();
    }

    void addMovedObject(String from, String to) {
        movedObjects.add(new MovedObject(from, to));
    }

    MoveRollbackSnapshot rollbackSnapshot() {
        return new MoveRollbackSnapshot(userId, List.copyOf(movedObjects));
    }
}
