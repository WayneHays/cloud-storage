package com.waynehays.cloudfilestorage.parser;

import com.waynehays.cloudfilestorage.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.exception.MultipartValidationException;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.validator.UploadObjectValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class UploadRequestParser {
    private final MultipartFileParser parser;
    private final UploadObjectValidator validator;

    public List<UploadObjectDto> parseAndValidate(List<MultipartFile> files, String directory) {
        List<UploadObjectDto> objects = files.stream()
                .map(file -> parser.parse(file, directory))
                .toList();

        List<String> errors = objects.stream()
                .map(validator::validate)
                .flatMap(Optional::stream)
                .toList();

        if (!errors.isEmpty()) {
            throw new MultipartValidationException(
                    "Multipart file validation failed: " + String.join("; ", errors));
        }

        checkDuplicates(objects);

        return objects;
    }

    private void checkDuplicates(List<UploadObjectDto> objects) {
        Set<String> seen = new HashSet<>();
        List<String> duplicates = objects.stream()
                .map(UploadObjectDto::fullPath)
                .filter(p -> !seen.add(p))
                .toList();

        if (!duplicates.isEmpty()) {
            throw new ResourceAlreadyExistsException("Duplicate paths in upload request", duplicates);
        }
    }
}