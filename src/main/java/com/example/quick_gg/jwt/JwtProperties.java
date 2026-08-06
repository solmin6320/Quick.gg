package com.example.quick_gg.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
@Component
public class JwtProperties {
    // JWT 발급자(iss) 정보
    private String issuer;

    // JWT 서명에 사용할 비밀키
    private String secret;

    // Access Token의 만료 시간
    private Long accessTokenExpiration;

    // Refresh Token의 만료 시간
    private Long refreshTokenExpiration;
}
