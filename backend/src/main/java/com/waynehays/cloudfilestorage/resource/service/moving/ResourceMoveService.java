package com.waynehays.cloudfilestorage.resource.service.moving;

import com.waynehays.cloudfilestorage.resource.dto.internal.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.resource.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.resource.mapper.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.resource.service.metadata.ResourceMetadataServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceMoveService implements ResourceMoveServiceApi {
    private final List<MoveStep> moveSteps;
    private final ResourceMetadataServiceApi metadataService;
    private final ResourceDtoMapper mapper;

    @Override
    public ResourceDto move(Long userId, String pathFrom, String pathTo) {
        log.info("Move started: userId={}, from={}, to={}", userId, pathFrom, pathTo);

        ResourceMetadataDto metadata = metadataService.findOrThrow(userId, pathFrom);
        MoveContext context = new MoveContext(userId, pathFrom, pathTo, metadata);
        List<MoveStep> executed = new ArrayList<>();

        for (MoveStep step : moveSteps) {
            try {
                step.execute(context);
                executed.add(step);
            } catch (Exception e) {
                log.warn("Move failed at step={}, userId={}, initiating rollback",
                        step.getClass().getSimpleName(), userId);
                MoveRollbackSnapshot snapshot = context.rollbackSnapshot();

                if (step.requiresRollback(snapshot)) {
                    executed.add(step);
                }

                rollback(executed, snapshot);
                throw e;
            }
        }

        log.info("Move completed: userId={}, from={}, to={}", userId, pathFrom, pathTo);
        return buildResult(context, metadata);
    }

    private void rollback(List<MoveStep> executed, MoveRollbackSnapshot snapshot) {
        ListIterator<MoveStep> iterator = executed.listIterator(executed.size());
        while (iterator.hasPrevious()) {
            MoveStep step = iterator.previous();
            try {
                step.rollback(snapshot);
            } catch (Exception e) {
                log.error("Rollback failed at step={}, userId={}",
                        step.getClass().getSimpleName(), snapshot.userId(), e);
            }
        }
    }

    private ResourceDto buildResult(MoveContext context, ResourceMetadataDto metadata) {
        return context.isMovingFile()
                ? mapper.fileFromPath(context.getPathTo(), metadata.size())
                : mapper.directoryFromPath(context.getPathTo());
    }
}
