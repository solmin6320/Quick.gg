package com.example.quick_gg.service;


import com.example.quick_gg.dto.response.TokenPair;
import com.example.quick_gg.entity.RefreshTokenEntity;
import com.example.quick_gg.entity.StudentEntity;
import com.example.quick_gg.exception.CustomException;
import com.example.quick_gg.exception.ErrorCode;
import com.example.quick_gg.jwt.JwtProperties;
import com.example.quick_gg.jwt.JwtTokenProvider;
import com.example.quick_gg.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefreshService {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserDetailsService userDetailsService;

    // 쿠키로 받은 refreshToken을 검증하고 액세스 토큰 + 리프레시 토큰 로테이션
    @Transactional
    public TokenPair reissue(String refreshToken) {

        // 쿠키가 아예 없는 경우
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // JWT 서명, 만료 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // DB에 살아있는 토큰인지 확인
        RefreshTokenEntity saved =
                refreshTokenRepository.findByToken(refreshToken).orElseThrow(
                        () -> new CustomException(ErrorCode.UNAUTHORIZED)
                );

        // 토큰 주인 조회
        StudentEntity student = saved.getStudent();

        // 토큰 생성에 필요한 인증 재구성
        Authentication authentication = buildAuthentication(student.getStudentNumber());

        // 사용한 refreshToken은 즉시 폐기
        refreshTokenRepository.delete(saved);

        // 새 토큰 쌍 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(authentication);

        String newRefreshToken = jwtTokenProvider.createRefreshToken(authentication);

        // 새 refreshToken 저장
        refreshTokenRepository.save(RefreshTokenEntity.builder()
                        .student(student)
                        .token(newRefreshToken)
                        .expiresAt(LocalDateTime.now().plusNanos(
                                jwtProperties.getRefreshTokenExpiration() * 1_000_000 // ms -> ns 변환
                        ))
                        .revoked(false)
                        .createdAt(LocalDateTime.now())
                        .build());

        return TokenPair.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    // 학번으로 UserDetails를 조회해 토큰 생성용 인증 객체를 만듬
    private Authentication buildAuthentication(String studentNumber) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(studentNumber);

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null, // 이미 인증된 사용자이기 때문에 자격증명 제외
                userDetails.getAuthorities()
        );
    }
}
