package com.waynehays.cloudfilestorage.files.api.exception;

import com.waynehays.cloudfilestorage.core.exception.ApplicationException;

public class MultipartParsingException extends ApplicationException {

    public MultipartParsingException(String message) {
        super(message);
    }
}
