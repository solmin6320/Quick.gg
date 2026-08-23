package com.example.quick_gg.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FavoriteRequest {

    // 소환사명 입력 검사
    @NotBlank(message = "소환사명을 입력해주세요")
    @Size(max = 50, message = "소환사명은 50자 이하여야 합니다")
    private String summonerName;

    // 태그 입력 검사
    @NotBlank(message = "태그를 입력해주세요")
    @Size(max = 10, message = "태그는 10자 이하여야 합니다")
    private String tag;
}
