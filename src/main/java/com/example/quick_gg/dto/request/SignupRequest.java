package com.example.quick_gg.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SignupRequest {

    // 학번 입력 검사
    @NotBlank(message = "학번을 입력해주세요")
    private String studentID;

    // 이름 입력 검사
    @NotBlank(message = "이름을 입력해주세요")
    private String name;

    // 비밀번호 입력 검사
    @NotBlank(message = "비밀번호를 입력해주세요")
    @Size(min = 2, max = 20, message = "비밀번호는 2자리 이상이어야 합니다")
    private String password;

    // 비밀번호 확인 검사
    @NotBlank(message = "비밀번호 확인을 입력해주세요")
    private String confirmPassword;

    // 소환사명 확인 검사
    @NotBlank(message = "소환사명을 입력해주세요")
    private String summonerName;

    // 태그 확인 검사
    @NotBlank(message = "태그를 입력해주세요")
    private String tag;

    // 인증코드 확인 검사
    @NotBlank(message = "학교 인증코드를 입력해주세요")
    private String verificationCode;
}
