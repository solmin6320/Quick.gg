package com.example.quick_gg.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;


@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    private final JwtProperties jwtProperties;

    private SecretKey secretKey;

    // 애플리케이션 시작 시 SecretKey를 한 번 생성
    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    // JWT 서명 및 검증에 사용할 SecretKey 반환
    public SecretKey getSigningKey() {
        return secretKey;
    }

    // 액세스 토큰 생성 메서드
    public String createAccessToken(Authentication authentication) {

        // 인증된 사용자 정보 조회
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 토큰 발급 시간 생성
        Date createAt = new Date();

        // Access Token 만료 시간 계산
        Date exp = new Date(
                createAt.getTime() + jwtProperties.getAccessTokenExpiration()
        );
            return Jwts.builder()
                    .subject(userDetails.getUsername())
                    .issuedAt(createAt)
                    .expiration(exp)
                    .issuer(jwtProperties.getIssuer())
                    .id(UUID.randomUUID().toString())
                    .signWith(secretKey)
                    .compact();
    }

    // 리프레쉬 토큰 생성 메서드
    public String createRefreshToken(Authentication authentication) {

        // 토큰 발급 시간 생성
        Date createAt = new Date();

        // Refresh Token 만료 시간 계산
        Date exp = new Date(
                createAt.getTime() + jwtProperties.getRefreshTokenExpiration()
        );

        // JWT 생성 및 반환
        return Jwts.builder()
                .subject(authentication.getName())
                .issuedAt(createAt)
                .expiration(exp)
                .issuer(jwtProperties.getIssuer())
                .id(UUID.randomUUID().toString())
                .signWith(secretKey)
                .compact();
    }
}
