package com.example.quick_gg.ranking;

import com.example.quick_gg.exception.CustomException;
import com.example.quick_gg.exception.ErrorCode;

// 기능명세서의 티어 점수표
// 최종점수 = 티어점수 + LP
public enum Tier {

    IRON(0),
    BRONZE(1000),
    SILVER(2000),
    GOLD(3000),
    PLATINUM(4000),
    EMERALD(5000),
    DIAMOND(6000),
    MASTER(7000),
    GRANDMASTER(8000),
    CHALLENGER(9000);

    private final int baseScore;

    Tier(int baseScore) {
        this.baseScore = baseScore;
    }

    public int getBaseScore() {
        return baseScore;
    }

    // DB에 저장된 티어 문자열(예: "gold")을 Tier로 변환
    // 정의되지 않은 값이 들어오면 400으로 처리
    public static Tier from(String tierName) {
        if (tierName == null || tierName.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        try {
            return Tier.valueOf(tierName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }
}
