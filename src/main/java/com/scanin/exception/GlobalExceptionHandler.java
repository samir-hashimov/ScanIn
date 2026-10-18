package com.scanin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. YENİ: DTO Validasiya xətalarını (@NotBlank, @Size, @Pattern) tutan metod:
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();

        // Hansı sahədə (field) hansı xəta baş veribsə hamısını toplayırıq:
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "VALIDATION_FAILED",
                "message", "Göndərilən məlumatlarda yanlışlıq var!",
                "details", fieldErrors,
                "timestamp", LocalDateTime.now()
        ));
    }

    // 2. İşçi ofis Wi-Fi-da deyilsə 403 FORBIDDEN
    @ExceptionHandler(InvalidIpException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidIp(InvalidIpException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", "IP_FORBIDDEN",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now()
        ));
    }

    // 3. Məlumat tapılmadıqda 404 NOT FOUND
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "NOT_FOUND",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now()
        ));
    }

    // 4. Biznes validasiya xətaları üçün (məs: 5 simvoldan az səbəb yazanda) 400 BAD REQUEST
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "BAD_REQUEST",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now()
        ));
    }
    // İstifadəçi adı və ya şifrə səhv olduqda 401 UNAUTHORIZED qaytarır:
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(Exception ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "UNAUTHORIZED",
                "message", "❌ İstifadəçi adı və ya şifrə yanlışdır!",
                "timestamp", LocalDateTime.now()
        ));
    }
}