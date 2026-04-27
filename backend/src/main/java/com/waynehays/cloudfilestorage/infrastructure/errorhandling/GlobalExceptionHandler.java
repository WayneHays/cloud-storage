package com.waynehays.cloudfilestorage.infrastructure.errorhandling;

import com.waynehays.cloudfilestorage.core.user.UserAlreadyExistsException;
import com.waynehays.cloudfilestorage.core.quota.exception.QuotaLimitException;
import com.waynehays.cloudfilestorage.core.quota.exception.QuotaNotFoundException;
import com.waynehays.cloudfilestorage.infrastructure.ratelimit.RateLimitException;
import com.waynehays.cloudfilestorage.files.operation.move.InvalidMoveException;
import com.waynehays.cloudfilestorage.files.api.resource.UploadValidationException;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.core.metadata.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.infrastructure.storage.ResourceStorageOperationException;
import com.waynehays.cloudfilestorage.infrastructure.storage.ResourceStorageTransientException;
import jakarta.validation.ConstraintViolationException;
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
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected @Nullable ResponseEntity<Object> handleHttpMessageNotReadable(@NotNull HttpMessageNotReadableException ex,
                                                                            @NotNull HttpHeaders headers,
                                                                            @NotNull HttpStatusCode status,
                                                                            @NotNull WebRequest request) {
        log.warn("Failed to parse JSON");
        ErrorDto error = createErrorDto("Invalid JSON format");
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

        log.warn("Validation failed: {}", message);
        ErrorDto error = createErrorDto(message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMissingServletRequestPart(@NotNull MissingServletRequestPartException ex,
                                                                               @NotNull HttpHeaders headers,
                                                                               @NotNull HttpStatusCode status,
                                                                               @NotNull WebRequest request) {
        log.warn("Empty or missing upload part");
        ErrorDto error = createErrorDto("Invalid resources to upload");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMaxUploadSizeExceededException(@NotNull MaxUploadSizeExceededException ex,
                                                                                    @NotNull HttpHeaders headers,
                                                                                    @NotNull HttpStatusCode status,
                                                                                    @NotNull WebRequest request) {
        log.warn("Too large size of uploaded resource");
        ErrorDto error = createErrorDto("File size is too large, max file size 500 MB");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(MultipartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDto handleMultipartException() {
        log.warn("Multipart error");
        return createErrorDto("Too many files or invalid upload request");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorDto handleDataIntegrityViolationException() {
        log.warn("Data integrity violation");
        return createErrorDto("Duplicate resource conflict");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDto handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("Constraint violation: {}", message);
        return createErrorDto(message);
    }

    @ExceptionHandler(QuotaNotFoundException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorDto handleStorageQuotaNotFoundException(QuotaNotFoundException e) {
        log.error("Storage quota not found", e);
        return createErrorDto("Storage quota not configured");
    }

    @ExceptionHandler(ResourceStorageTransientException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorDto handleResourceStorageTransientException(ResourceStorageTransientException e) {
        log.error("Transient storage error", e);
        return createErrorDto("Storage temporarily unavailable, please try again");
    }

    @ExceptionHandler(QuotaLimitException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDto handleResourceStorageLimitException(QuotaLimitException e) {
        log.warn("Not enough storage space. Upload size: {}, free space: {}",
                e.getUploadSize(), e.getFreeSpace());
        return createErrorDto("Not enough storage space. Free: %s, required: %s".formatted(
                formatBytes(e.getFreeSpace()),
                formatBytes(e.getUploadSize())
        ));
    }

    @ExceptionHandler(RateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ErrorDto handleRateLimitException(RateLimitException e) {
        log.warn("Rate limit exceeded: endpoint={}, method={}, retryAfter={}s",
                e.getEndpoint(), e.getHttpMethod(), e.getRetryAfter());
        return createErrorDto(e.getMessage());
    }

    @ExceptionHandler(InvalidMoveException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDto handleInvalidMoveException(InvalidMoveException e) {
        log.warn("Invalid move: '{}' -> '{}'", e.getFrom(), e.getTo());
        String clientMessage = "%s: '%s' -> '%s'".formatted(e.getMessage(), e.getFrom(), e.getTo());
        return createErrorDto(clientMessage);
    }

    @ExceptionHandler(UploadValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDto handleMultipartValidationException(UploadValidationException e) {
        log.warn("Multipart validation failed: {}", e.getMessage());
        return createErrorDto(e.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorDto handleResourceNotFoundException(ResourceNotFoundException e) {
        log.warn("Resource not found: '{}'", e.getPath());
        return createErrorDto("Resource not found: '%s'".formatted(e.getPath()));
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorDto handleResourceAlreadyExistsException(ResourceAlreadyExistsException e) {
        List<String> paths = e.getPaths();
        int maxPathsToSendWithMessage = 5;
        String message;

        if (paths.size() <= maxPathsToSendWithMessage) {
            log.warn("Resources already exist: {}", paths);
            message = "Resources already exist: %s".formatted(paths);
        } else {
            log.warn("Resources already exist: count={}, first={}",
                    paths.size(), paths.subList(0, maxPathsToSendWithMessage));
            message = "Resources already exist: %d files, e.g. %s"
                    .formatted(paths.size(), paths.subList(0, maxPathsToSendWithMessage));
        }
        return createErrorDto(message);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorDto handleUserAlreadyExistsException(UserAlreadyExistsException e) {
        log.warn("Username already taken: {}", e.getMessage());
        return createErrorDto(e.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorDto handleBadCredentialsException() {
        log.warn("Failed authentication attempt");
        return createErrorDto("Invalid credentials");
    }

    @ExceptionHandler(ResourceStorageOperationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorDto handleResourceStorageOperationException(ResourceStorageOperationException e) {
        log.error("Storage operation failed", e);
        return createErrorDto("Failed to process operation");
    }

    @ExceptionHandler(ApplicationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorDto handleApplicationException(ApplicationException e) {
        log.error("Unhandled application exception: {}", e.getMessage(), e);
        return createErrorDto(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorDto handleException(Exception e) {
        log.error("Unexpected exception", e);
        return createErrorDto("Internal server error");
    }

    private ErrorDto createErrorDto(String message) {
        return new ErrorDto(message);
    }

    private String formatBytes(long bytes) {
        if (bytes >= 1024 * 1024 * 1024) {
            return "%.1f GB".formatted(bytes / (1024.0 * 1024 * 1024));
        } else if (bytes >= 1024 * 1024) {
            return "%.1f MB".formatted(bytes / (1024.0 * 1024));
        }
        return "%.1f KB".formatted(bytes / 1024.0);
    }
}
