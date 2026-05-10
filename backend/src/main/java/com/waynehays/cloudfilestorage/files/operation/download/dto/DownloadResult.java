package com.waynehays.cloudfilestorage.files.operation.download.dto;

import org.springframework.core.io.InputStreamSource;

import java.io.IOException;
import java.io.OutputStream;

public sealed interface DownloadResult {

    String name();

    String contentType();

    record File(InputStreamSource contentSupplier, String name, String contentType) implements DownloadResult {
    }

    record Archive(StreamWriter writer, String name, String contentType) implements DownloadResult {
    }

    @FunctionalInterface
    interface StreamWriter {
        void writeTo(OutputStream outputStream) throws IOException;
    }
}
