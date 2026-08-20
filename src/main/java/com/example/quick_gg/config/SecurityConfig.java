package com.example.quick_gg.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.example.quick_gg.security.JwtAuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // BCrypt를 사용하는 PasswordEncoder 등록
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 로그인 시 아이디, 비밀번호 인증을 처리
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
    throws Exception {
        return configuration.getAuthenticationManager();
    }
    
    
    // 보안 규칙(필터 체인)을 정의
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                // CORS 추가
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // CSRF 방어 비활성화(쿠키만으로 인증이 완성되지 않음)
                .csrf(csrf -> csrf.disable())


                // 세션 저장소를 사용하지 않고, 매 요청마다 JWT로만 인증
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // 경로별 인가 규칙 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/signup", "/auth/login")
                        .permitAll()
                        .anyRequest().authenticated() // 회원가입, 로그인 제외 모든 경로는 인증된 사용자만 접근 가능
                )
                // Jwt 인증 필터를 기본 필터 보다 먼저 실행
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return httpSecurity.build();


    }

    // CORS 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
         CorsConfiguration configuration = new CorsConfiguration();

         configuration.setAllowedOrigins(List.of("https://quick-gg-front.com")); // 프론트 도메인(이미 이름을 정했음)
         configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")); // 허용할  HTTP 메서드 목록
         configuration.setAllowedHeaders(List.of("*")); // 요청 시 허용할 헤더
         configuration.setAllowCredentials(true); // 쿠키를 주고받을 수 있도록 허용
         configuration.setMaxAge(3600L); // 요청 결과를 브라우저가 개시(초)

        // 위 설정을 모든 경로에 동일하게 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
