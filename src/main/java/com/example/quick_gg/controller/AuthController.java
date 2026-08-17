package com.example.quick_gg.controller;

import com.example.quick_gg.dto.request.LoginRequest;
import com.example.quick_gg.dto.request.SignupRequest;
import com.example.quick_gg.dto.response.LoginResponse;
import com.example.quick_gg.dto.response.SignupResponse;
import com.example.quick_gg.dto.response.TokenPair;
import com.example.quick_gg.jwt.JwtProperties;
import com.example.quick_gg.service.LoginService;
import com.example.quick_gg.service.SignupService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    // 이름 임의 지정
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private final SignupService signupService;
    private final LoginService loginService;
    private final JwtProperties jwtProperties; // 쿠키 만료시간을 원본값에서 가져옴

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(
            @RequestBody SignupRequest request) {

        SignupResponse response = signupService.signup(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request, HttpServletResponse response) {

        // access, refresh 토큰을 받음
        TokenPair tokenPair = loginService.login(request);

        // refreshToken을 httpOnly 쿠키로 만들어 응답 헤더에 추가
        ResponseCookie cookie = buildRefreshTokenCookie(tokenPair.getRefreshToken());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());


        // 응답 바디에는 accessToken만 포함
        LoginResponse loginResponse = LoginResponse.builder()
                .accessToken(tokenPair.getAccessToken())
                .build();

        return ResponseEntity.ok(loginResponse);
    }

    // refreshToken 쿠키 생성
    private ResponseCookie buildRefreshTokenCookie(String token) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, token)
                .httpOnly(true) // js가 직접 못 건들게 만듬(XSS 공격 방어)
                .secure(true) // secure 에서만 주고 받음
                // 다른 도메인에서 요헝해도 쿠키를 실어보냄(CSRF 공격 취약 <-> 백엔드, 프론트 별도 배포)
                .sameSite("None") // 트레이드오프
                .path("/")
                .maxAge(Duration.ofMillis(jwtProperties.getRefreshTokenExpiration()))
                .build();
    }

}
