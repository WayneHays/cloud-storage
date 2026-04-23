package com.waynehays.cloudfilestorage.service.resource.move;

import com.waynehays.cloudfilestorage.dto.internal.metadata.ResourceMetadataDto;
import com.waynehays.cloudfilestorage.dto.response.ResourceDto;
import com.waynehays.cloudfilestorage.mapper.ResourceDtoMapper;
import com.waynehays.cloudfilestorage.service.metadata.ResourceMetadataServiceApi;
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
                rollback(executed, context.rollbackSnapshot());
                throw e;
            }
        }

        log.info("Move completed: userId={}, from={}, to={}", userId, pathFrom, pathTo);
        return buildResult(context, metadata);
    }

    private void rollback(List<MoveStep> executed, MoveRollbackSnapshot snapshot) {
        ListIterator<MoveStep> it = executed.listIterator(executed.size());
        while (it.hasPrevious()) {
            MoveStep step = it.previous();
            try {
                step.rollback(snapshot);
            } catch (Exception e) {
                log.error("Rollback failed at step={}, userId={}",
                        step.getClass().getSimpleName(), snapshot.userId(), e);
            }
        }
    }

    private ResourceDto buildResult(MoveContext context, ResourceMetadataDto metadata) {
        return context.isFile()
                ? mapper.fileFromPath(context.getPathTo(), metadata.size())
                : mapper.directoryFromPath(context.getPathTo());
    }
}
