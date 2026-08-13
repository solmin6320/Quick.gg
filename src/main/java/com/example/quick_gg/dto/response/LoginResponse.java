package com.example.quick_gg.dto.response;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
@Getter
@AllArgsConstructor
public class LoginResponse {

    // 로그인한 학생의 PK
    private Integer id;

    // API 요청 인증용 JWT
    private String accessToken;

    // 액세스 토큰 재발급용
    private String refreshToken;

    // 로그인 시각
    private LocalDateTime loginAt;
}
