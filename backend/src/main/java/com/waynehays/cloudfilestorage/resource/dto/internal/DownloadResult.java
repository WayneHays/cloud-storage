package com.waynehays.cloudfilestorage.resource.dto.internal;

import com.waynehays.cloudfilestorage.storage.service.InputStreamSupplier;

import java.io.IOException;
import java.io.OutputStream;

public sealed interface DownloadResult {

    String name();

    String contentType();

    record File(InputStreamSupplier contentSupplier, String name, String contentType) implements DownloadResult {
    }

    record Archive(StreamWriter writer, String name, String contentType) implements DownloadResult {
    }

    @FunctionalInterface
    interface StreamWriter {
        void writeTo(OutputStream outputStream) throws IOException;
    }
}
