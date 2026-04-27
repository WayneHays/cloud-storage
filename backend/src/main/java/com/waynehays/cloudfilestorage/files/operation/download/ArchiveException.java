package com.waynehays.cloudfilestorage.files.operation.download;

import com.waynehays.cloudfilestorage.infrastructure.errorhandling.ApplicationException;

public class ArchiveException extends ApplicationException {

    ArchiveException(String message, Throwable cause) {
        super(message, cause);
    }
}
