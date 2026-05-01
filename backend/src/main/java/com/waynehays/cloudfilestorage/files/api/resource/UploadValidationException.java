package com.waynehays.cloudfilestorage.files.api.resource;

import com.waynehays.cloudfilestorage.infrastructure.errorhandling.ApplicationException;
import lombok.Getter;

import java.util.List;

@Getter
public class UploadValidationException extends ApplicationException {
    private final List<String> errors;

    UploadValidationException(List<String> errors) {
        this.errors = errors;
    }

    UploadValidationException(String error) {
        this.errors = List.of(error);
    }
}
