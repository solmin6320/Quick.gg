package com.example.quick_gg.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SignupResponse {

    // 가입된 학생의 PK
    private Integer id;

    // 학번 (로그인 ID로 사용)
    private String studentID;

    // 학생 이름
    private String name;

    // 등록한 소환사명
    private String summonerName;

    // 소환사 태그 (예: KR1)
    private String tag;

    // 가입 완료 시각
    private LocalDateTime createAt;

}
