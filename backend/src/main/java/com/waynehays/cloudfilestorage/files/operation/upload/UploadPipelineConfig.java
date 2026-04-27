package com.waynehays.cloudfilestorage.files.operation.upload;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
class UploadPipelineConfig {

    @Bean
    List<UploadStep> uploadSteps(
            ValidateStep validate,
            ReserveQuotaStep reserveQuota,
            StorageUploadStep storageUpload,
            SaveMetadataStep saveMetadata,
            CreateDirectoriesStep createDirectories
    ) {
        return List.of(validate, reserveQuota, storageUpload, saveMetadata, createDirectories);
    }
}
