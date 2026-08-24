package com.example.quick_gg.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginRequest {

    // 학번 입력 검사
    @NotBlank(message = "학번을 입력해주세요")
    private String studentNumber;

    // 비밀번호 입력 검사
    @NotBlank(message = "비밀번호를 입력해주세요")
    @Size(min = 2, max = 20, message = "비밀번호는 2자리 이상이어야 합니다")
    private String password;
}
