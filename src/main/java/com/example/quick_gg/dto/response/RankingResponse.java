package com.example.quick_gg.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
// 학교 랭킹 목록의 한 줄에 해당하는 응답
public class RankingResponse {

    // 학교 랭킹 내 순위 (1위부터)
    private Integer position;

    // 소환사명
    private String summonerName;

    // 소환사 태그 (예: KR1)
    private String tag;

    // "학번 이름(마스킹)" 형태 (예: 21103 김**)
    private String maskedStudentInfo;

    // 티어 (예: GOLD)
    private String tier;

    // 디비전 (예: II)
    private String rankTier;

    // 리그 포인트
    private Integer lp;

    // 티어 점수 + LP
    private Integer score;

    // 승 수
    private Integer wins;

    // 패 수
    private Integer losses;

    // 승률(%), 소수 첫째 자리까지
    private Double winRate;
}
