package com.waynehays.cloudfilestorage.resource.service.moving;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class MovePipelineConfig {

    @Bean
    List<MoveStep> moveSteps(
            ValidateMoveStep validate,
            MoveStorageStep moveStorage,
            MoveMetadataStep moveMetadata
    ) {
        return List.of(validate, moveStorage, moveMetadata);
    }
}
