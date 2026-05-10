package com.waynehays.cloudfilestorage.files.operation.download.dto;

import org.springframework.core.io.InputStreamSource;

public record ArchiveItem(String name, long size, InputStreamSource inputStreamSource) {
}
