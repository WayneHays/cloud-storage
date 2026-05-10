package com.waynehays.cloudfilestorage.files.operation.upload.exception;

import com.waynehays.cloudfilestorage.core.exception.ApplicationException;
import lombok.Getter;

import java.util.List;

@Getter
public class UploadValidationException extends ApplicationException {
    private final List<String> errors;

    public UploadValidationException(List<String> errors) {
        this.errors = errors;
    }
}
