package com.example.quick_gg.security;

import com.example.quick_gg.entity.StudentEntity;
import com.example.quick_gg.repository.StudentRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final StudentRepository studentRepository;

    // StudentRepository 생성자 주입
    public CustomUserDetailsService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    // 학번으로 사용자를 조회하여 Spring Security에 전달
    @Override
    public UserDetails loadUserByUsername(String studentID) throws UsernameNotFoundException {
        StudentEntity studentEntity = studentRepository.findByStudentNumber(studentID).orElseThrow(() -> // 람다식 구현
                new UsernameNotFoundException("학생을 찾을 수 없습니다"));

        // 조회한 사용자를 CustomUserDetails로 변환하여 반환
        return new CustomUserDetails(studentEntity);
    }
}
