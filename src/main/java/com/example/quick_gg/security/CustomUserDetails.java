package com.example.quick_gg.security;

import com.example.quick_gg.entity.StudentEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final StudentEntity studentEntity;

    // 사용자의 정보를 저장
    public CustomUserDetails(StudentEntity studentEntity) {
        this.studentEntity = studentEntity;
    }

    // 사용자의 로그인 ID(학번)를 반환
    @Override
    public String getUsername() {
        return studentEntity.getStudentID();
    }

    // 사용자의 비밀번호를 반환
    @Override
    public String getPassword() {
        return studentEntity.getPassword();
    }

    // 사용자의 권한을 반환
    // 현재 프로젝트는 관리자, 매니저 같은 역할이 없으므로 유저 권한으로 통일
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_USER")
        );
    }
}
