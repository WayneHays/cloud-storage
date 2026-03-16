package com.waynehays.cloudfilestorage.dto.response;

import java.util.List;

public record ErrorDto(
        List<String> messages
) {
    public ErrorDto(String message) {
        this(List.of(message));
    }
}
