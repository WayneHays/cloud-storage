package com.waynehays.cloudfilestorage.files.operation.upload;

import com.waynehays.cloudfilestorage.files.api.dto.response.ResourceResponse;
import com.waynehays.cloudfilestorage.files.operation.upload.dto.Context;
import com.waynehays.cloudfilestorage.files.operation.upload.dto.RollbackDto;
import com.waynehays.cloudfilestorage.files.operation.upload.dto.UploadObjectDto;
import com.waynehays.cloudfilestorage.files.operation.upload.step.UploadStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
class ResourceUploadService implements ResourceUploadServiceApi {
    private final UploadPipeline pipeline;

    @Override
    public List<ResourceResponse> upload(Long userId, List<UploadObjectDto> objects) {
        log.info("Upload started: files count={}", objects.size());

        Context context = new Context(userId, objects);
        List<UploadStep> executedSteps = new ArrayList<>();

        for (UploadStep step : pipeline.getSteps()) {
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

        List<ResourceResponse> result = context.getResult();
        log.info("Upload completed: files count={}", result.size());
        return result;
    }

    private void rollback(List<UploadStep> executedSteps, RollbackDto rollbackDto) {
        for (UploadStep step : executedSteps.reversed()) {
            try {
                step.rollback(rollbackDto);
            } catch (Exception e) {
                log.error("Rollback failed at step={}", step.getClass().getSimpleName(), e);
            }
        }
    }
}