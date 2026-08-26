package com.example.quick_gg.service;

import com.example.quick_gg.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LogoutService {

    private final RefreshTokenRepository refreshTokenRepository;

    // 쿠키로 받은 refreshToken을 DB에서 삭제
    @Transactional
    public void logout(String refreshToken) {

        // 쿠키가 없거나 이미 로그아웃 된 상태여도 성공으로 처리
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        refreshTokenRepository.deleteByToken(refreshToken);
        }
}
