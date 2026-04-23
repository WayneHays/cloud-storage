package com.waynehays.cloudfilestorage.resource.service.uploading;

import com.waynehays.cloudfilestorage.resource.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.resource.dto.response.ResourceDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceUploadService implements ResourceUploadServiceApi {
    private final List<UploadStep> uploadSteps;

    @Override
    public List<ResourceDto> upload(Long userId, List<UploadObjectDto> objects) {
        log.info("Upload started: userId={}, files={}", userId, objects.size());

        UploadContext context = new UploadContext(userId, objects);
        List<UploadStep> executed = new ArrayList<>();

        for (UploadStep step : uploadSteps) {
            try {
                step.execute(context);
                executed.add(step);
            } catch (Exception e) {
                log.warn("Upload failed at step={}, userId={}, initiating rollback",
                        step.getClass().getSimpleName(), userId);
                UploadRollbackSnapshot snapshot = context.rollbackSnapshot();

                if (step.requiresRollback(snapshot)) {
                    executed.add(step);
                }

                rollback(executed, snapshot);
                throw e;
            }
        }

        log.info("Upload completed: userId={}, result={}", userId, context.getResult().size());
        return context.getResult();
    }

    private void rollback(List<UploadStep> executed, UploadRollbackSnapshot snapshot) {
        ListIterator<UploadStep> iterator = executed.listIterator(executed.size());
        while (iterator.hasPrevious()) {
            UploadStep step = iterator.previous();
            try {
                step.rollback(snapshot);
            } catch (Exception e) {
                log.error("Rollback failed at step={}, userId={}",
                        step.getClass().getSimpleName(), snapshot.userId(), e);
            }
        }
    }
}