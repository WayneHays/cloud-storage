package com.waynehays.cloudfilestorage.shared.exception;

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
