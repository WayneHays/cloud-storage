package com.waynehays.cloudfilestorage.exceptionhandler;

import com.waynehays.cloudfilestorage.dto.response.ErrorDto;
import com.waynehays.cloudfilestorage.exception.EmptyFileException;
import com.waynehays.cloudfilestorage.exception.FileStorageException;
import com.waynehays.cloudfilestorage.exception.InvalidMoveException;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.exception.UserAlreadyExistsException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@Slf4j
@RestControllerAdvice(basePackages = "com.waynehays.cloudfilestorage.controller")
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final String LOG_EMPTY_UPLOAD = "Attempt to upload empty or invalid resource";
    private static final String LOG_SIZE_EXCEEDED = "File size limit exceeded";
    private static final String LOG_INVALID_CREDENTIALS = "Invalid credentials attempt";

    private static final String MSG_INVALID_UPLOAD = "Invalid resources to upload";
    private static final String MSG_SIZE_EXCEEDED = "File size exceeds the allowed limit";
    private static final String MSG_INVALID_MOVE = "Invalid move operation - can't move directory to file";
    private static final String MSG_EMPTY_FILE = "File cannot be empty";
    private static final String MSG_NOT_FOUND = "Resource not found";
    private static final String MSG_ALREADY_EXISTS = "Resource already exists";
    private static final String MSG_USERNAME_TAKEN = "Username already taken";
    private static final String MSG_INVALID_CREDENTIALS = "Invalid credentials";
    private static final String MSG_STORAGE_ERROR = "Failed to process file operation";
    private static final String MSG_INTERNAL_ERROR = "Internal server error";

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                            @NotNull HttpHeaders headers,
                                                                            @NotNull HttpStatusCode status,
                                                                            @NotNull WebRequest request) {
        List<String> messages = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return new ResponseEntity<>(new ErrorDto(messages), HttpStatus.BAD_REQUEST);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMissingServletRequestPart(@NotNull MissingServletRequestPartException ex,
                                                                               @NotNull HttpHeaders headers,
                                                                               @NotNull HttpStatusCode status,
                                                                               @NotNull WebRequest request) {
        log.info(LOG_EMPTY_UPLOAD);
        return buildBadRequestResponse(MSG_INVALID_UPLOAD);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMaxUploadSizeExceededException(@NotNull MaxUploadSizeExceededException ex,
                                                                                    @NotNull HttpHeaders headers,
                                                                                    @NotNull HttpStatusCode status,
                                                                                    @NotNull WebRequest request) {
        log.info(LOG_SIZE_EXCEEDED);
        return buildBadRequestResponse(MSG_SIZE_EXCEEDED);
    }

    @ExceptionHandler(InvalidMoveException.class)
    public ResponseEntity<ErrorDto> handleInvalidMoveException(InvalidMoveException e) {
        log.info(e.getMessage());
        return buildErrorResponse(MSG_INVALID_MOVE, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EmptyFileException.class)
    public ResponseEntity<ErrorDto> handleEmptyFile(EmptyFileException e) {
        log.info(e.getMessage());
        return buildErrorResponse(MSG_EMPTY_FILE, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorDto> handleResourceNotFound(ResourceNotFoundException e) {
        log.info(e.getMessage());
        return buildErrorResponse(MSG_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ErrorDto> handleResourceAlreadyExists(ResourceAlreadyExistsException e) {
        log.info(e.getMessage());
        return buildErrorResponse(MSG_ALREADY_EXISTS, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorDto> handleUserAlreadyExists(UserAlreadyExistsException e) {
        log.info(e.getMessage());
        return buildErrorResponse(MSG_USERNAME_TAKEN, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorDto> handleBadCredentials(BadCredentialsException e) {
        log.info(LOG_INVALID_CREDENTIALS);
        return buildErrorResponse(MSG_INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorDto> handleFileStorageException(FileStorageException e) {
        log.error(MSG_STORAGE_ERROR, e);
        return buildErrorResponse(MSG_STORAGE_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleException(Exception e) {
        log.error(MSG_INTERNAL_ERROR, e);
        return buildErrorResponse(MSG_INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorDto> buildErrorResponse(String message, HttpStatus status) {
        return ResponseEntity.status(status).body(new ErrorDto(message));
    }

    private ResponseEntity<Object> buildBadRequestResponse(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto(message));
    }
}
