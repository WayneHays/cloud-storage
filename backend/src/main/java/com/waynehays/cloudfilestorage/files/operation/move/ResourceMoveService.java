package com.waynehays.cloudfilestorage.files.operation.move;

import com.waynehays.cloudfilestorage.core.metadata.ResourceMetadataServiceApi;
import com.waynehays.cloudfilestorage.core.metadata.dto.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.files.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.files.operation.ResourceDtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

@Slf4j
@Service
@RequiredArgsConstructor
class ResourceMoveService implements ResourceMoveServiceApi {
    private final List<MoveStep> moveSteps;
    private final ResourceMetadataServiceApi metadataService;
    private final ResourceDtoMapper mapper;

    @Override
    public ResourceDto move(Long userId, String pathFrom, String pathTo) {
        log.info("Move started: from={}, to={}", pathFrom, pathTo);

        ResourceMetadataDto metadata = metadataService.findOrThrow(userId, pathFrom);
        MoveContext context = new MoveContext(userId, pathFrom, pathTo);
        List<MoveStep> executed = new ArrayList<>();

        for (MoveStep step : moveSteps) {
            try {
                step.execute(context);
                executed.add(step);
            } catch (Exception e) {
                log.warn("Move failed at step={}, initiating rollback",
                        step.getClass().getSimpleName());
                MoveRollbackDto snapshot = context.rollbackSnapshot();

                if (step.requiresRollback(snapshot)) {
                    executed.add(step);
                }

                rollback(executed, snapshot);
                throw e;
            }
        }

        log.info("Move completed: from={}, to={}", pathFrom, pathTo);
        return buildResult(context, metadata);
    }

    private void rollback(List<MoveStep> executed, MoveRollbackDto snapshot) {
        ListIterator<MoveStep> iterator = executed.listIterator(executed.size());
        while (iterator.hasPrevious()) {
            MoveStep step = iterator.previous();
            try {
                step.rollback(snapshot);
            } catch (Exception e) {
                log.error("Rollback failed at step={}",
                        step.getClass().getSimpleName(), e);
            }
        }
    }

    private ResourceDto buildResult(MoveContext context, ResourceMetadataDto metadata) {
        return metadata.isFile()
                ? mapper.fileFromPath(context.getPathTo(), metadata.size())
                : mapper.directoryFromPath(context.getPathTo());
    }
}
