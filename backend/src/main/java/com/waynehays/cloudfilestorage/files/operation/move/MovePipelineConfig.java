package com.waynehays.cloudfilestorage.files.operation.move;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
class MovePipelineConfig {

    @Bean
    List<MoveStep> moveSteps(
            ValidateMoveStep validate,
            MoveStorageStep moveStorage,
            MoveMetadataStep moveMetadata
    ) {
        return List.of(validate, moveStorage, moveMetadata);
    }
}
