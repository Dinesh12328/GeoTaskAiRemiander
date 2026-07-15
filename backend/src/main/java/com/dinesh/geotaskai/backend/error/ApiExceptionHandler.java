package com.dinesh.geotaskai.backend.error;

import com.dinesh.geotaskai.backend.task.TaskNotFoundException;
import java.time.Clock;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private final Clock clock;

    public ApiExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(TaskNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(TaskNotFoundException error) {
        return buildResponse(HttpStatus.NOT_FOUND, error.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException error) {
        List<String> details = error.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
            .toList();
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadableBody() {
        return buildResponse(HttpStatus.BAD_REQUEST, "Request body is missing or invalid", List.of());
    }

    private ResponseEntity<ApiError> buildResponse(
        HttpStatus status,
        String message,
        List<String> details
    ) {
        return ResponseEntity.status(status)
            .body(new ApiError(clock.instant(), status.value(), message, details));
    }
}
