package com.example.quick_gg.exception;

import com.example.quick_gg.dto.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

// 전역 예외 처리기
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 공통 응답 생성 메서드
    private ResponseEntity<ErrorResponse> buildResponse(ErrorCode errorCode, String message) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.builder()
                        .message(message) // 예외 메세지
                        .timestamp(LocalDateTime.now()) // 예외가 터진 시간
                        .build());
    }


    // CustomException 처리 (예: 404, 403 등등)
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        log.warn(e.getMessage());

        return buildResponse(e.getErrorCode(), e.getMessage());
    }

    // @Valid 검증 실패 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        // @RequestBody 검증 실패 시 첫 번째 필드의 오류 메시지를 가져옴
        String message = e.getBindingResult().getFieldError().getDefaultMessage();

        log.warn(message);

        return buildResponse(ErrorCode.INVALID_INPUT, message);
    }

    // @Validate 검증 실패 처리
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException e) {
        // @RequestParam, @PathVariable 검증 실패 시 첫 번째 오류 메시지를 가져옴
        String message = e.getConstraintViolations().iterator().next().getMessage();

        log.warn(message);

        return buildResponse(ErrorCode.INVALID_INPUT, message);
    }

    // 예상하지 못한 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {

        log.error("예상하지 못한 예외 발생", e);

        return buildResponse(ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
    }
    }

