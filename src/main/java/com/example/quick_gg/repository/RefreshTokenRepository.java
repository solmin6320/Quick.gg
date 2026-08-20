package com.example.quick_gg.repository;

import com.example.quick_gg.entity.RefreshTokenEntity;
import com.example.quick_gg.entity.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Integer> {

    // 재발급 요청 시 토큰 문자열로 유효성 검증
    Optional<RefreshTokenEntity> findByToken(String token);

    // 로그아웃 요청 시 해당 토큰만 삭제
    void deleteByToken(String token);

    // 로그인 시, 그 학생의 기존 토큰을 전부 삭제 (다중 로그인과 트레이드오프)
    void deleteByStudent(StudentEntity student);

}
