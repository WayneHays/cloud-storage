package com.waynehays.cloudfilestorage.files.operation.move.exception;

import com.waynehays.cloudfilestorage.core.exception.ApplicationException;
import lombok.Getter;

@Getter
public class InvalidMoveException extends ApplicationException {
    private final String from;
    private final String to;

    public InvalidMoveException(String message, String from, String to) {
        super(message);
        this.from = from;
        this.to = to;
    }
}
