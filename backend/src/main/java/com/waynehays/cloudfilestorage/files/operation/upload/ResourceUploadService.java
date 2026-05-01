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

        UploadContext context = new UploadContext(userId, objects);
        List<UploadStep> executed = new ArrayList<>();

        for (UploadStep step : uploadSteps) {
            try {
                step.execute(context);
                executed.add(step);
            } catch (Exception e) {
                log.warn("Upload failed at step={}, initiating rollback",
                        step.getClass().getSimpleName());
                UploadRollbackDto snapshot = context.rollbackDto();

                if (step.requiresRollback(snapshot)) {
                    executed.add(step);
                }

                rollback(executed, snapshot);
                throw e;
            }
        }

        log.info("Upload completed: files count={}", context.getResult().size());
        return context.getResult();
    }

    private void rollback(List<UploadStep> executed, UploadRollbackDto snapshot) {
        ListIterator<UploadStep> iterator = executed.listIterator(executed.size());
        while (iterator.hasPrevious()) {
            UploadStep step = iterator.previous();
            try {
                step.rollback(snapshot);
            } catch (Exception e) {
                log.error("Rollback failed at step={}",
                        step.getClass().getSimpleName(), e);
            }
        }
    }
}