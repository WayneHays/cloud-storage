package com.waynehays.cloudfilestorage.shared.utils;

import com.waynehays.cloudfilestorage.shared.exception.ApplicationException;
import com.waynehays.cloudfilestorage.shared.exception.ResourceStorageOperationException;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@UtilityClass
public class AsyncUtils {

    public static void joinAll(List<? extends CompletableFuture<?>> futures, String errorMessage) {
        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        } catch (CompletionException e) {
            futures.forEach(f -> f.cancel(true));
            Throwable cause = e.getCause();

            if (cause instanceof ApplicationException ae) {
                throw ae;
            }
            throw new ResourceStorageOperationException(errorMessage, cause);
        }
    }
}
