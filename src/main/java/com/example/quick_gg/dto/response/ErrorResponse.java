package com.example.quick_gg.dto.response;

import com.example.quick_gg.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
// 클라이언트에게 주는 에러 응답 형식
public class ErrorResponse {

    // 예외 메세지
    private String message;
    // 예외가 발생한 시각
    private LocalDateTime timestamp;

    // 실제로는 buildResponse()를 통해 응답을 만들고 있어 실제로는 사용하지 않는 상태
//    서비스 로직 등 다른 곳에서 ErrorCode 기반으로 바로 응답을 만들 때 활용 가능하므로 유지.
    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .message(errorCode.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }
}
