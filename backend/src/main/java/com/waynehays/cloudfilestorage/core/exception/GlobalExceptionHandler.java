package com.waynehays.cloudfilestorage.core.exception;

import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.core.quota.exception.QuotaLimitException;
import com.waynehays.cloudfilestorage.core.quota.exception.QuotaNotFoundException;
import com.waynehays.cloudfilestorage.core.user.exception.UserAlreadyExistsException;
import com.waynehays.cloudfilestorage.files.operation.move.exception.InvalidMoveException;
import com.waynehays.cloudfilestorage.files.operation.upload.config.ResourceLimitsProperties;
import com.waynehays.cloudfilestorage.files.operation.upload.exception.UploadValidationException;
import com.waynehays.cloudfilestorage.infrastructure.storage.exception.ResourceStorageException;
import com.waynehays.cloudfilestorage.infrastructure.storage.exception.ResourceStorageTransientException;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final long BYTES_PER_MEGABYTE = 1024 * 1024L;
    private static final long BYTES_PER_GIGABYTE = 1024 * BYTES_PER_MEGABYTE;
    private static final int MAX_PATHS_IN_MESSAGE = 5;

    private final ResourceLimitsProperties limitsProperties;

    @Override
    protected @Nullable ResponseEntity<Object> handleHttpMessageNotReadable(@NotNull HttpMessageNotReadableException ex,
                                                                            @NotNull HttpHeaders headers,
                                                                            @NotNull HttpStatusCode status,
                                                                            @NotNull WebRequest request) {
        ErrorResponse error = createErrorResponse("Invalid JSON format");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                            @NotNull HttpHeaders headers,
                                                                            @NotNull HttpStatusCode status,
                                                                            @NotNull WebRequest request) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ErrorResponse error = createErrorResponse(message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMissingServletRequestPart(@NotNull MissingServletRequestPartException ex,
                                                                               @NotNull HttpHeaders headers,
                                                                               @NotNull HttpStatusCode status,
                                                                               @NotNull WebRequest request) {
        log.warn("Empty or missing upload part");
        ErrorResponse error = createErrorResponse("Invalid resources to upload");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMaxUploadSizeExceededException(@NotNull MaxUploadSizeExceededException ex,
                                                                                    @NotNull HttpHeaders headers,
                                                                                    @NotNull HttpStatusCode status,
                                                                                    @NotNull WebRequest request) {
        log.warn("Too large size of uploaded resource");
        String maxSize = limitsProperties.maxFileSize().toMegabytes() + " MB";
        ErrorResponse error = createErrorResponse("File size is too large, max file size " + maxSize);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MultipartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMultipartException(MultipartException e) {
        log.warn("Multipart error: {}", e.getMessage());
        return createErrorResponse("Too many files or invalid upload request");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.warn("Data integrity violation: {}", e.getMostSpecificCause().getMessage());
        return createErrorResponse("Duplicate resource conflict");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        return createErrorResponse(message);
    }

    @ExceptionHandler(QuotaNotFoundException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleQuotaNotFoundException(QuotaNotFoundException e) {
        log.error("Storage quota not found: userId={}", e.getUserId());
        return createErrorResponse("Storage quota not configured");
    }

    @ExceptionHandler(QuotaLimitException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleQuotaLimitException(QuotaLimitException e) {
        return createErrorResponse("Not enough storage space. Free: %s, required: %s".formatted(
                formatBytes(e.getFreeSpace()),
                formatBytes(e.getUploadSize())
        ));
    }

    @ExceptionHandler(ResourceStorageTransientException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleResourceStorageTransientException(ResourceStorageTransientException e) {
        log.error("Transient storage error", e);
        return createErrorResponse("Storage temporarily unavailable, please try again");
    }

    @ExceptionHandler(InvalidMoveException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidMoveException(InvalidMoveException e) {
        String message = "%s: '%s' -> '%s'".formatted(e.getMessage(), e.getFrom(), e.getTo());
        return createErrorResponse(message);
    }

    @ExceptionHandler(UploadValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUploadValidationException(UploadValidationException e) {
        return createErrorResponse(e.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleResourceNotFoundException(ResourceNotFoundException e) {
        return createErrorResponse("Resource not found: '%s'".formatted(e.getPath()));
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleResourceAlreadyExistsException(ResourceAlreadyExistsException e) {
        List<String> paths = e.getPaths();
        String message;

        if (paths.size() <= MAX_PATHS_IN_MESSAGE) {
            message = "Resources already exist: %s".formatted(paths);
        } else {
            message = "Resources already exist: %d files, e.g. %s"
                    .formatted(paths.size(), paths.subList(0, MAX_PATHS_IN_MESSAGE));
        }
        return createErrorResponse(message);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleUserAlreadyExistsException(UserAlreadyExistsException e) {
        return createErrorResponse(e.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleBadCredentialsException() {
        return createErrorResponse("Invalid credentials");
    }

    @ExceptionHandler(ResourceStorageException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleResourceStorageOperationException(ResourceStorageException e) {
        log.error("Storage operation failed", e);
        return createErrorResponse("Failed to process operation");
    }

    @ExceptionHandler(ApplicationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleApplicationException(ApplicationException e) {
        log.error("Unhandled application exception: {}", e.getMessage(), e);
        return createErrorResponse(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleException(Exception e) {
        log.error("Unexpected exception", e);
        return createErrorResponse("Internal server error");
    }

    private ErrorResponse createErrorResponse(String message) {
        return new ErrorResponse(message);
    }

    private String formatBytes(long bytes) {
        if (bytes >= BYTES_PER_GIGABYTE) {
            return "%.1f GB".formatted((double) bytes / BYTES_PER_GIGABYTE);
        } else if (bytes >= BYTES_PER_MEGABYTE) {
            return "%.1f MB".formatted((double) bytes / BYTES_PER_MEGABYTE);
        }
        return "%.1f KB".formatted(bytes / 1024.0);
    }
}
