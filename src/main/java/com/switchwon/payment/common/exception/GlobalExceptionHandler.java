package com.switchwon.payment.common.exception;

import com.switchwon.payment.common.response.ApiResponse;
import com.switchwon.payment.common.response.ResponseCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException e) {
        ResponseCode code = e.getResponseCode();
        return ResponseEntity.status(code.getHttpStatus())
                .body(ApiResponse.error(code, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse(ResponseCode.INVALID_REQUEST.getDefaultMessage());
        return ResponseEntity.status(ResponseCode.INVALID_REQUEST.getHttpStatus())
                .body(ApiResponse.error(ResponseCode.INVALID_REQUEST, message));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestHeader(MissingRequestHeaderException e) {
        String message = e.getHeaderName() + " 헤더가 필요합니다";
        return ResponseEntity.status(ResponseCode.INVALID_REQUEST.getHttpStatus())
                .body(ApiResponse.error(ResponseCode.INVALID_REQUEST, message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse(ResponseCode.INVALID_REQUEST.getDefaultMessage());
        return ResponseEntity.status(ResponseCode.INVALID_REQUEST.getHttpStatus())
                .body(ApiResponse.error(ResponseCode.INVALID_REQUEST, message));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException e) {
        return ResponseEntity.status(ResponseCode.NOT_FOUND.getHttpStatus())
                .body(ApiResponse.error(ResponseCode.NOT_FOUND, "요청하신 경로를 찾을 수 없습니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(ResponseCode.SYSTEM_ERROR.getHttpStatus())
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR));
    }
}