package com.example.quick_gg.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "school_rank")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Builder
public class SchoolRankEntity {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 학생 1명당 랭킹 정보 1건 (1:1)
    // student_id 에 UNIQUE 제약이 걸려 있으므로 OneToOne 으로 매핑
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private StudentEntity student;

    // 티어 (예: GOLD)
    @Column(nullable = false, length = 20)
    private String tier;

    // 디비전 (예: II) — 마스터 이상은 값이 없을 수 있어 nullable
    @Column(name = "rank_tier", length = 5)
    private String rankTier;

    // 리그 포인트
    @Column(nullable = false)
    private Integer lp;

    // 승 수
    @Column(nullable = false)
    private Integer wins;

    // 패 수
    @Column(nullable = false)
    private Integer losses;

    // 마지막 갱신 시각
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 갱신 시 엔티티를 새로 만들지 않고 필드 값만 교체
    public void updateRank(String tier,
                           String rankTier,
                           Integer lp,
                           Integer wins,
                           Integer losses) {
        this.tier = tier;
        this.rankTier = rankTier;
        this.lp = lp;
        this.wins = wins;
        this.losses = losses;
        this.updatedAt = LocalDateTime.now();
    }
}
