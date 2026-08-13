package com.example.quick_gg.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
// 복합 유니크 처리(summoner_name, tag)
@Table(name = "student",
uniqueConstraints = {
        @UniqueConstraint(
                columnNames = {
                        "summoner_name", "tag"
                }
        )
})
// 생성자, 게터 어노테이션 추가
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class StudentEntity {

    @Id
    // DB에 ID값 자동 처리 위임
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "student_number",nullable = false, unique = true, length = 10)
    private String studentID;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false)
    private String password;

    @Column(name = "summoner_name", nullable = false, length = 50)
    private String summonerName;

    @Column(nullable = false, length = 100, unique = true)
    private String puuid;

    @Column(nullable = false, length = 10)
    private String tag;

    @Column(name = "created_at",nullable = false,updatable = false)
    private LocalDateTime createAt;
}
