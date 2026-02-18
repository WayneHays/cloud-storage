package com.waynehays.cloudfilestorage.exceptionhandler;

import com.waynehays.cloudfilestorage.dto.response.ErrorDto;
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
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice(basePackages = "com.waynehays.cloudfilestorage.controller")
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        log.debug("Validation params for method failed");
        return new ResponseEntity<>(new ErrorDto("Validation failed"), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorDto> handleUserAlreadyExists(UserAlreadyExistsException e) {
        String message = e.getMessage();
        log.info(message, e);
        return new ResponseEntity<>(new ErrorDto(message), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorDto> handleBadCredentials(BadCredentialsException e) {
        String message = "Invalid credentials";
        log.info(message, e);
        return new ResponseEntity<>(new ErrorDto(message), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleException(Exception e) {
        log.error("Internal server error", e);
        return new ResponseEntity<>(new ErrorDto("Internal server error"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
