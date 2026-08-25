package com.example.quick_gg.service;

import com.example.quick_gg.dto.request.LoginRequest;
import com.example.quick_gg.dto.response.LoginResponse;
import com.example.quick_gg.dto.response.TokenPair;
import com.example.quick_gg.entity.RefreshTokenEntity;
import com.example.quick_gg.entity.StudentEntity;
import com.example.quick_gg.exception.CustomException;
import com.example.quick_gg.exception.ErrorCode;
import com.example.quick_gg.jwt.JwtProperties;
import com.example.quick_gg.jwt.JwtTokenProvider;
import com.example.quick_gg.repository.RefreshTokenRepository;
import com.example.quick_gg.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties; // 추가 주입
    private final RefreshTokenRepository refreshTokenRepository;
    private final StudentRepository studentRepository;

    // 학번 + 비밀번호로 인증 시도
    public TokenPair login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getStudentNumber(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            // 학번 또는 비밀번호 불일치
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // 인증된 학생 엔티티 조회
        StudentEntity student = studentRepository.findByStudentNumber(request.getStudentNumber())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        // 기존 refreshToken 삭제 (다중 기기 포기, 항상 최신 로그인만 유효)
        refreshTokenRepository.deleteByStudent(student);

        // 새 accessToken, refreshToken 발급
        String accessToken = jwtTokenProvider.createAccessToken(authentication);
        String refreshToken = jwtTokenProvider.createRefreshToken(authentication);

        // 새 refreshToken 엔티티 생성 및 저장
        RefreshTokenEntity refreshTokenEntity = RefreshTokenEntity.builder()
                .student(student)
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusNanos(
                        jwtProperties.getRefreshTokenExpiration() * 1_000_000 // ms -> ns 변환
                ))
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        // accessToken만 응답으로 반환
        return TokenPair.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}