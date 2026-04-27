package com.waynehays.cloudfilestorage.files.operation.move;

import com.waynehays.cloudfilestorage.infrastructure.errorhandling.ApplicationException;
import lombok.Getter;

@Getter
public class InvalidMoveException extends ApplicationException {
    private final String from;
    private final String to;

    InvalidMoveException(String message, String from, String to) {
        super(message);
        this.from = from;
        this.to = to;
    }
}
