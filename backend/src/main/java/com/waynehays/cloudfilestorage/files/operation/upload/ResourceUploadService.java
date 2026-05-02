package com.waynehays.cloudfilestorage.files.operation.upload;

import com.waynehays.cloudfilestorage.files.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.files.dto.response.ResourceDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

@Slf4j
@Service
@RequiredArgsConstructor
class ResourceUploadService implements ResourceUploadServiceApi {
    private final List<UploadStep> uploadSteps;

    @Override
    public List<ResourceDto> upload(Long userId, List<UploadObjectDto> objects) {
        log.info("Upload started: files count={}", objects.size());

        Context context = new Context(userId, objects);
        List<UploadStep> executedSteps = new ArrayList<>();

        for (UploadStep step : uploadSteps) {
            try {
                step.execute(context);
                executedSteps.add(step);
            } catch (Exception e) {
                log.warn("Upload failed at step={}, initiating rollback",
                        step.getClass().getSimpleName());
                RollbackDto rollbackDto = context.rollbackDto();

                if (step.requiresRollback(rollbackDto)) {
                    executedSteps.add(step);
                }

                rollback(executedSteps, rollbackDto);
                throw e;
            }
        }

        log.info("Upload completed: files count={}", context.getResult().size());
        return context.getResult();
    }

    private void rollback(List<UploadStep> executedSteps, RollbackDto rollbackDto) {
        ListIterator<UploadStep> iterator = executedSteps.listIterator(executedSteps.size());
        while (iterator.hasPrevious()) {
            UploadStep step = iterator.previous();
            try {
                step.rollback(rollbackDto);
            } catch (Exception e) {
                log.error("Rollback failed at step={}",
                        step.getClass().getSimpleName(), e);
            }
        }
    }
}