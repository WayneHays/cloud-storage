package com.waynehays.cloudfilestorage.parser;

import com.waynehays.cloudfilestorage.dto.internal.UploadObjectDto;
import com.waynehays.cloudfilestorage.exception.MultipartValidationException;
import com.waynehays.cloudfilestorage.validator.UploadObjectValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

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
                    "Validation failed: " + String.join("; ", errors));
        }

        return objects;
    }
}