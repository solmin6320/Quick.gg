package com.example.quick_gg.service;

import com.example.quick_gg.entity.StudentEntity;
import com.example.quick_gg.exception.CustomException;
import com.example.quick_gg.exception.ErrorCode;
import com.example.quick_gg.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
// 현재 로그인한 학생을 조회하는 공통 컴포넌트
// JwtAuthenticationFilter가 SecurityContext에 넣어둔 인증 정보를 사용
public class CurrentStudentProvider {

    private final StudentRepository studentRepository;

    public StudentEntity getCurrentStudent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 인증 정보가 없으면 401
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // JWT의 subject(= 학번)로 학생 조회
        String studentID = authentication.getName();

        return studentRepository.findByStudentNumber(studentID)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
    }
}
