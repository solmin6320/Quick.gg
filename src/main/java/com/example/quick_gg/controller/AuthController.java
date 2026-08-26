package com.example.quick_gg.controller;

import com.example.quick_gg.dto.request.LoginRequest;
import com.example.quick_gg.dto.request.SignupRequest;
import com.example.quick_gg.dto.response.LoginResponse;
import com.example.quick_gg.dto.response.SignupResponse;
import com.example.quick_gg.dto.response.TokenPair;
import com.example.quick_gg.jwt.JwtProperties;
import com.example.quick_gg.service.LoginService;
import com.example.quick_gg.service.LogoutService;
import com.example.quick_gg.service.RefreshService;
import com.example.quick_gg.service.SignupService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    // 이름 임의 지정
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private final RefreshService refreshService;
    private final SignupService signupService;
    private final LoginService loginService;
    private final JwtProperties jwtProperties; // 쿠키 만료시간을 원본값에서 가져옴
    private final LogoutService logoutService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(
            @Valid
            @RequestBody SignupRequest request) {

        SignupResponse response = signupService.signup(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid
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


    // 액세스 토큰 재발급
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@CookieValue(value = REFRESH_TOKEN_COOKIE_NAME,
            required = false) String refreshToken, HttpServletResponse response) {

            // 쿠키의 refreshToken으로 새 토큰 쌍을 발급받음
        TokenPair tokenPair =
                refreshService.reissue(refreshToken);

        // 회전된 refreshToken을 다시 쿠키로 내려줌
        ResponseCookie cookie = buildRefreshTokenCookie(tokenPair.getRefreshToken());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // 응답 바디에는 액세스 토큰만 포함
        return ResponseEntity.ok(LoginResponse.builder()
                        .accessToken(tokenPair.getAccessToken())
                        .build());
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken, HttpServletResponse response) {

        // DB에 저장된 refreshToken 제거
        logoutService.logout(refreshToken);

        // 브라우저에 남아있는 쿠키도 즉시 만료
        ResponseCookie cookie = buildExpiredRefreshTokenCookie();

        // 쿠키를 문자열 형태로 변환해 헤더로 반환
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok().build();
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

    // 로그아웃 시 브라우저의 리프레시 토큰을 즉시 만료
    private ResponseCookie buildExpiredRefreshTokenCookie() {

        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true) // js가 직접 못 건들게 만듬(XSS 방어)
                .secure(true) // secure 에서만 주고 받음
                .sameSite("None")
                .path("/")
                .maxAge(0) // 0초 -> 브라우저가 쿠키를 즉시 삭제
                .build();
    }


}
