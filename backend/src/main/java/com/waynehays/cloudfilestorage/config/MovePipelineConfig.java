package com.waynehays.cloudfilestorage.config;

import com.waynehays.cloudfilestorage.service.resource.move.MoveMetadataStep;
import com.waynehays.cloudfilestorage.service.resource.move.MoveStep;
import com.waynehays.cloudfilestorage.service.resource.move.MoveStorageStep;
import com.waynehays.cloudfilestorage.service.resource.move.ValidateMoveStep;
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
