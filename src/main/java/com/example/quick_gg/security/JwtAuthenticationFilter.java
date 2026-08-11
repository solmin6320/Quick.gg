package com.example.quick_gg.security;

import com.example.quick_gg.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {


    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws IOException, ServletException {
        // 요청 헤더에서 순수 토큰 문자열만 추출
        String token = resolveToken(request);

        // 토큰이 존재하고 서명, 만료 검증을 통과한 경우에만 인증 처리
        if (jwtTokenProvider != null && jwtTokenProvider.validateToken(token)) {

            // 토큰을 파싱해서 Claims 반환
            Claims claims = jwtTokenProvider.parseClaims(token);

            // Claims 에서 subject(studentID) 꺼내기
            String studentID = claims.getSubject();

            // DB에서 studentID로 사용자 정보 조회
            UserDetails userDetails =  userDetailsService.loadUserByUsername(studentID);

            // 인증 객체 생성
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                // 이 요청을 인증된 사용자로 등록
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        // 토큰 유무와 상관없이 다음 필터로 요청 전달
        filterChain.doFilter(request, response);

    }

    private String resolveToken(HttpServletRequest request) {
        // Authorization 헤더 값 가져오기
        String bearerToken = request.getHeader("Authorization");

        // Bearer 로 시작하는지 확인 (Bearer 토큰 방식이기 때문)
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // 공백 제외 순수 문자열 반환
        }
        return null; // 헤더가 없거나 형식이 다르면 null
    }
}
