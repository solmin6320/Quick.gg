package com.example.quick_gg.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_token")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Builder
public class RefreshTokenEntity {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 여러 RefreshToken이 한 Student에 속하는 관계 (N:1)
    // 로그인 시 기존 토큰을 삭제하므로 실제로는 학생당 1개만 유지 (다중 로그인 기능과 트레이드오프)
    @ManyToOne(fetch = FetchType.LAZY) // 필요 시점에만 조회 (지연 로딩 fetch 전략)
    @JoinColumn(name = "student_id", nullable = false) // 실제 fk 컬럼명
    private StudentEntity student;

    // 토큰
    @Column(nullable = false, unique = true, length = 500)
    private String token;

    // 만료 시각
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // 토큰 무효화 여부 (서비스 로직에서 기본 false)
    @Column(nullable = false)
    private Boolean revoked = false;

    // 생성 시각
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
