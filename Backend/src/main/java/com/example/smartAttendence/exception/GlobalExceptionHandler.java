package com.example.smartAttendence.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.EntityNotFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<?> handleContentTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "UNSUPPORTED_MEDIA_TYPE", 
            "Please use 'Content-Type: application/json' header. All API endpoints require JSON.");
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> handleNotFound(EntityNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to perform this action.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationErrors(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Invalid request data: " + details);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgs(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
        String message = ex.getMessage();
        
        if (message != null && (message.contains("Security") || message.contains("Constraint"))) {
            return buildResponse(HttpStatus.BAD_REQUEST, "SECURITY_OR_CONSTRAINT_VIOLATION", message);
        }

        // Log unexpected runtime errors with a tracking ID
        String refId = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        logger.error("[{}] Runtime Error: {}", refId, ex.getMessage(), ex);

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", 
            "An unexpected error occurred. Reference ID: " + refId);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDatabaseException(DataIntegrityViolationException ex) {
        return buildResponse(HttpStatus.CONFLICT, "DATABASE_ERROR", "Data conflict or integrity violation. This usually happens when an entry already exists.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex) {
        String refId = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        logger.error("[{}] Global Exception: {}", refId, ex.getMessage(), ex);
        
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_ERROR", 
            "A system error occurred. Please contact support with Ref ID: " + refId);
    }

    private ResponseEntity<?> buildResponse(HttpStatus status, String code, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("code", code);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
