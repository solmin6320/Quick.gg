package com.example.quick_gg.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
// 복합 유니크 처리(student_id, summoner_name, tag)
@Table(name = "favorite_summoner",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "student_id", "summoner_name", "tag"
                        }
                )
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Builder
public class FavoriteSummonerEntity {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 한 학생이 여러 즐겨찾기를 가짐 (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEntity student;

    // 즐겨찾기한 소환사명
    @Column(name = "summoner_name", nullable = false, length = 50)
    private String summonerName;

    // 즐겨찾기한 소환사 태그 (예: KR1)
    @Column(nullable = false, length = 10)
    private String tag;
}
