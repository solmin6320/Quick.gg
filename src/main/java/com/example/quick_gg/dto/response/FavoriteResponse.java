package com.example.quick_gg.dto.response;

import com.example.quick_gg.entity.FavoriteSummonerEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
// 즐겨찾기 응답
public class FavoriteResponse {

    // 즐겨찾기 PK (삭제 시 사용)
    private Integer id;

    // 소환사명
    private String summonerName;

    // 소환사 태그
    private String tag;

    // 엔티티 -> 응답 DTO 변환
    public static FavoriteResponse from(FavoriteSummonerEntity favorite) {
        return FavoriteResponse.builder()
                .id(favorite.getId())
                .summonerName(favorite.getSummonerName())
                .tag(favorite.getTag())
                .build();
    }
}
