package com.example.quick_gg.dto.response;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
// 서비스 -> 컨트롤러 사이에서만 쓰이는 내부 전달용 DTO
public class TokenPair {

    private String accessToken;
    private String refreshToken;
}
