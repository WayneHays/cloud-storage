package com.waynehays.cloudfilestorage;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CloudFileStorageApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(CloudFileStorageApplication.class)
                .run(args);
    }
}
