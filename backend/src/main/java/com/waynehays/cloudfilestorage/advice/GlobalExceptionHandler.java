package com.waynehays.cloudfilestorage.advice;

import com.waynehays.cloudfilestorage.dto.response.ErrorDto;
import com.waynehays.cloudfilestorage.exception.InvalidMoveException;
import com.waynehays.cloudfilestorage.exception.MultipartValidationException;
import com.waynehays.cloudfilestorage.exception.RateLimitException;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.exception.ResourceStorageLimitException;
import com.waynehays.cloudfilestorage.exception.ResourceStorageOperationException;
import com.waynehays.cloudfilestorage.exception.ResourceStorageTransientException;
import com.waynehays.cloudfilestorage.exception.StorageQuotaNotFoundException;
import com.waynehays.cloudfilestorage.exception.UserAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.UserNotFoundException;
import com.waynehays.cloudfilestorage.security.CustomUserDetails;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected @Nullable ResponseEntity<Object> handleHttpMessageNotReadable(@NotNull HttpMessageNotReadableException ex,
                                                                            @NotNull HttpHeaders headers,
                                                                            @NotNull HttpStatusCode status,
                                                                            @NotNull WebRequest request) {
        log.error("Failed to parse JSON: {}", ex.getMessage());
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

        log.warn("Validation failed for user: {}, {}", getCurrentUserInfo(), message);
        ErrorDto error = createErrorDto(message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMissingServletRequestPart(@NotNull MissingServletRequestPartException ex,
                                                                               @NotNull HttpHeaders headers,
                                                                               @NotNull HttpStatusCode status,
                                                                               @NotNull WebRequest request) {
        log.warn("Empty or missing upload part for user: {}", getCurrentUserInfo());
        ErrorDto error = createErrorDto("Invalid resources to upload");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMaxUploadSizeExceededException(@NotNull MaxUploadSizeExceededException ex,
                                                                                    @NotNull HttpHeaders headers,
                                                                                    @NotNull HttpStatusCode status,
                                                                                    @NotNull WebRequest request) {
        log.warn("Too large size of uploaded resource: {}", getCurrentUserInfo());
        ErrorDto error = createErrorDto("File size is too large");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorDto handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.warn("Data integrity violation: {}, {}", getCurrentUserInfo(), e.getMostSpecificCause().getMessage());
        return createErrorDto("Duplicate resource conflict");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDto handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("Constraint violation: {}, {}", getCurrentUserInfo(), message);
        return createErrorDto(message);
    }

    @ExceptionHandler(StorageQuotaNotFoundException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorDto handleStorageQuotaNotFoundException(StorageQuotaNotFoundException e) {
        log.error("Storage quota not found: userId={}", e.getUserId(), e);
        return createErrorDto("Storage quota not configured");
    }

    @ExceptionHandler(ResourceStorageTransientException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorDto handleResourceStorageTransientException(ResourceStorageTransientException e) {
        log.error("Transient storage error: {}", getCurrentUserInfo(), e);
        return createErrorDto("Storage temporarily unavailable, please try again");
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorDto handleUserNotFoundException(UserNotFoundException e) {
        log.warn("User not found: userId={}", e.getUserId());
        return createErrorDto("User not found");
    }

    @ExceptionHandler(ResourceStorageLimitException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDto handleResourceStorageLimitException(ResourceStorageLimitException e) {
        log.warn("Not enough storage space for user: {}. Upload size: {}, free space: {}",
                getCurrentUserInfo(), e.getUploadSize(), e.getFreeSpace());
        return createErrorDto("Not enough storage space. Free space: " + e.getFreeSpace());
    }

    @ExceptionHandler(RateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ErrorDto handleRateLimitException(RateLimitException e) {
        log.warn("Rate limit exceeded: {}, endpoint={}, method={}, retryAfter={}s",
                getCurrentUserInfo(), e.getEndpoint(), e.getHttpMethod(), e.getRetryAfter());
        return createErrorDto(e.getMessage());
    }

    @ExceptionHandler(InvalidMoveException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDto handleInvalidMoveException(InvalidMoveException e) {
        log.warn("{}: {}, '{}' -> '{}'", e.getMessage(), getCurrentUserInfo(), e.getFrom(), e.getTo());
        String clientMessage = "Cannot move directory to file: '%s' -> '%s'".formatted(e.getFrom(), e.getTo());
        return createErrorDto(clientMessage);
    }

    @ExceptionHandler(MultipartValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDto handleMultipartValidationException(MultipartValidationException e) {
        log.warn("Multipart file validation failed for user: {}, {}", getCurrentUserInfo(), e.getMessage());
        return createErrorDto(e.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorDto handleResourceNotFoundException(ResourceNotFoundException e) {
        log.warn("{}: {}, path='{}'", e.getMessage(), getCurrentUserInfo(), e.getPath());
        return createErrorDto("Resource not found: " + e.getPath());
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorDto handleResourceAlreadyExistsException(ResourceAlreadyExistsException e) {
        log.warn("{}: {}, paths={}", e.getMessage(), getCurrentUserInfo(), e.getPaths());
        return createErrorDto("Resources already exists: " + e.getPaths());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorDto handleUserAlreadyExistsException(UserAlreadyExistsException e) {
        log.warn("{}", e.getMessage());
        return createErrorDto(e.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorDto handleBadCredentialsException(BadCredentialsException e) {
        log.warn("Failed authentication attempt", e);
        return createErrorDto("Invalid credentials");
    }

    @ExceptionHandler(ResourceStorageOperationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorDto handleResourceStorageOperationException(ResourceStorageOperationException e) {
        log.error("{}: {}", e.getMessage(), getCurrentUserInfo(), e);
        return createErrorDto("Failed to process operation");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorDto handleException(Exception e) {
        log.error("Unexpected exception: {}", getCurrentUserInfo(), e);
        return createErrorDto("Internal server error");
    }

    private ErrorDto createErrorDto(String message) {
        return new ErrorDto(message);
    }

    private UserInfo getCurrentUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails user) {
            return new UserInfo(user.id(), user.username());
        }
        return new UserInfo(null, "anonymous");
    }

    private record UserInfo(Long id, String username) {
        @Override
        public @NonNull String toString() {
            return "user='%s', userId=%s".formatted(username, id != null ? id : "N/A");
        }
    }
}
