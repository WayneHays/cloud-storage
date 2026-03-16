package com.waynehays.cloudfilestorage.exceptionhandler;

import com.waynehays.cloudfilestorage.dto.response.ErrorDto;
import com.waynehays.cloudfilestorage.exception.EmptyFileException;
import com.waynehays.cloudfilestorage.exception.FileStorageException;
import com.waynehays.cloudfilestorage.exception.InvalidMoveException;
import com.waynehays.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.waynehays.cloudfilestorage.exception.ResourceNotFoundException;
import com.waynehays.cloudfilestorage.exception.UserAlreadyExistsException;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        List<String> messages = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return new ResponseEntity<>(new ErrorDto(messages), HttpStatus.BAD_REQUEST);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMissingServletRequestPart(MissingServletRequestPartException ex,
                                                                               HttpHeaders headers,
                                                                               HttpStatusCode status,
                                                                               WebRequest request) {
        log.info("Attempt to upload empty or invalid resource");
        return ResponseEntity.badRequest().body(new ErrorDto("Invalid resources to upload"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorDto> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.info("File size limit exceeded");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorDto("File size exceeds allowed limit"));
    }

    @ExceptionHandler(InvalidMoveException.class)
    public ResponseEntity<ErrorDto> handleInvalidMoveException(InvalidMoveException e) {
        log.info(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto("Invalid move operation - can't move directory to file"));
    }

    @ExceptionHandler(EmptyFileException.class)
    public ResponseEntity<ErrorDto> handleEmptyFile(EmptyFileException e) {
        log.info(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto("File cannot be empty"));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorDto> handleResourceNotFound(ResourceNotFoundException e) {
        log.info(e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto("Resource not found"));
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ErrorDto> handleResourceAlreadyExists(ResourceAlreadyExistsException e) {
        log.info(e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorDto("Resource already exists"));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorDto> handleUserAlreadyExists(UserAlreadyExistsException e) {
        log.info(e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorDto("Username already taken"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorDto> handleBadCredentials(BadCredentialsException e) {
        log.info("Invalid credentials attempt");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorDto("Invalid credentials"));
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorDto> handleFileStorageException(FileStorageException e) {
        log.error("File storage error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorDto("Failed to process file operation"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleException(Exception e) {
        log.error("Internal server error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorDto("Internal server error"));
    }
}
