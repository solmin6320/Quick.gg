package com.example.quick_gg.service;

import com.example.quick_gg.dto.request.SignupRequest;
import com.example.quick_gg.dto.response.SignupResponse;
import com.example.quick_gg.entity.StudentEntity;
import com.example.quick_gg.exception.CustomException;
import com.example.quick_gg.exception.ErrorCode;
import com.example.quick_gg.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class SignupService {
    private final StudentRepository repository;
    private final PasswordEncoder passwordEncoder;

    //  회원 생성
    public SignupResponse signup(SignupRequest request) {
        // 학번 중복 검사
        if (repository.existsByStudentNumber(request.getStudentNumber())) {
            throw new CustomException(ErrorCode.CONFLICT);
        }

        // 비밀번호 확인 일치 검사
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // 비밀번호 길이 검사
        int len = request.getPassword().length();
        if (len < 6 || len > 20) {
            throw  new CustomException(ErrorCode.INVALID_INPUT);
        }

        // 소환사(이름+태그) 중복 검사
        if (repository.existsBySummonerNameAndTag(request.getSummonerName(), request.getTag())) {
            throw new CustomException(ErrorCode.CONFLICT);
        }

        String encodePassword = passwordEncoder.encode(request.getPassword());

        // DB 저장용
        StudentEntity student = StudentEntity.builder()
                .studentNumber(request.getStudentNumber())
                .name(request.getName())
                .password(encodePassword)
                .summonerName(request.getSummonerName())
                .puuid(puuid) // 라이엇 API 연동 로직이 아직 완성되지 않아 컴파일 에러(고칠 예정)
                .tag(request.getTag())
                .createAt(LocalDateTime.now())
                .build();

        // DB에 실제 저장
        repository.save(student);


        // 응답 반환용(비밀번호 제외)
        return SignupResponse.builder()
                .studentNumber(student.getStudentNumber())
                .name(student.getName())
                .summonerName(student.getSummonerName())
                .tag(student.getTag())
                .createAt(student.getCreateAt())
                .build();

    }
}
