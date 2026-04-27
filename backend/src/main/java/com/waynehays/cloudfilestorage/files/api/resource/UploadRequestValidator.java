package com.waynehays.cloudfilestorage.files.api.resource;

import com.waynehays.cloudfilestorage.infrastructure.path.ResourceLimitsProperties;
import com.waynehays.cloudfilestorage.files.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.infrastructure.path.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
class UploadRequestValidator {
    private final ResourceLimitsProperties properties;

    void validate(List<UploadObjectDto> objects) {
        validateEachObject(objects);
        validateNoDuplicates(objects);
    }

    private void validateEachObject(List<UploadObjectDto> objects) {
        List<String> errors = objects.stream()
                .map(this::validate)
                .flatMap(Optional::stream)
                .toList();

        if (!errors.isEmpty()) {
            throw new UploadValidationException(
                    "Upload objects validation failed: " + String.join("; ", errors));
        }
    }

    private void validateNoDuplicates(List<UploadObjectDto> objects) {
        Set<String> seen = new HashSet<>();
        List<String> duplicates = objects.stream()
                .map(UploadObjectDto::fullPath)
                .filter(p -> !seen.add(p))
                .toList();

        if (!duplicates.isEmpty()) {
            throw new ResourceAlreadyExistsException("Duplicate paths in upload request", duplicates);
        }
    }

    private Optional<String> validate(UploadObjectDto dto) {
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
