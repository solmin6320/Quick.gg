package com.example.quick_gg.exception;

import lombok.Getter;

@Getter
// 서비스 전역에서 사용하는 커스텀 예외
public class CustomException extends RuntimeException {
    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
