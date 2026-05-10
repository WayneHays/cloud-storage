package com.waynehays.cloudfilestorage.files.operation.upload;

import com.waynehays.cloudfilestorage.files.operation.upload.step.UploadStep;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Component
public class UploadPipeline {
    private final List<UploadStep> steps;

    public UploadPipeline(List<UploadStep> steps) {
        this.steps = List.copyOf(steps);
    }
}
