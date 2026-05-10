package com.waynehays.cloudfilestorage.files.operation.upload.config;

import com.waynehays.cloudfilestorage.files.operation.upload.UploadPipeline;
import com.waynehays.cloudfilestorage.files.operation.upload.step.CreateDirectoriesStep;
import com.waynehays.cloudfilestorage.files.operation.upload.step.ReserveQuotaStep;
import com.waynehays.cloudfilestorage.files.operation.upload.step.SaveMetadataStep;
import com.waynehays.cloudfilestorage.files.operation.upload.step.StorageUploadStep;
import com.waynehays.cloudfilestorage.files.operation.upload.step.ValidateStep;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class UploadPipelineConfig {

    @Bean
    UploadPipeline uploadPipeline(
            ValidateStep validate,
            ReserveQuotaStep reserveQuota,
            StorageUploadStep storageUpload,
            SaveMetadataStep saveMetadata,
            CreateDirectoriesStep createDirectories
    ) {
        return new UploadPipeline(
                List.of(validate, reserveQuota, storageUpload, saveMetadata, createDirectories)
        );
    }
}
