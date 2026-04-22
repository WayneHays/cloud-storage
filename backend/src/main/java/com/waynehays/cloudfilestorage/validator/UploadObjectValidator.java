package com.waynehays.cloudfilestorage.validator;

import com.waynehays.cloudfilestorage.config.properties.ResourceLimitsProperties;
import com.waynehays.cloudfilestorage.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UploadObjectValidator {
    private final ResourceLimitsProperties properties;

    public Optional<String> validate(UploadObjectDto dto) {
        String filename = dto.filename();

        if (ValidationUtils.isInvalidInput(filename)) {
            return Optional.of("'%s': contains invalid characters".formatted(filename));
        }
        if (filename.length() > properties.maxFilenameLength()) {
            return Optional.of("'%s': exceeds max filename length of %d".formatted(
                    filename, properties.maxFilenameLength()));
        }
        if (dto.fullPath().length() > properties.maxPathLength()) {
            return Optional.of("'%s': full path exceeds max length of %d".formatted(
                    filename, properties.maxPathLength()));
        }
        if (dto.size() > properties.maxFileSize().toBytes()) {
            return Optional.of("'%s': exceeds max size of %s".formatted(
                    filename, properties.maxFileSize()));
        }
        return Optional.empty();
    }
}
