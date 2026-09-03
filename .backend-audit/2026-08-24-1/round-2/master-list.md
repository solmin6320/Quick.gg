# Round 2 통합 이슈 리스트 (main 브랜치 병합 반영)

기준 커밋: `main` @ `386c2fa` (라운드 1 기준이었던 `feature/auth` @ `1202f2e`에서 `feature/auth`+`feature/ranking-favorite` PR이 모두 머지된 이후 상태)

**제외 항목 (라운드 1에서 이미 사용자 결정 완료, 재질문하지 않음)**: C-1(puuid 컴파일 에러), C-2(application.yml 부재), H-1(인증코드 미검증), H-2(회원가입 TOCTOU), H-3(JwtAuthenticationFilter 예외 미처리) — 모두 "보류(계획된 향후 작업)"로 확정됨. 코드 재확인 결과 해당 5개 지점은 `git diff`상 변경되지 않아 그대로 유효하다.

---

## High

### NH-1. 학교 랭킹 API가 전체 학생의 로그인 아이디(학번)를 마스킹 없이 노출 [신규]
- **관련**: dependency 없음(신규 코드), 관련 스킬 성격상 auth-audit + error-audit 교집합
- **위치**: `RankingService.java:108`, `RankingResponse.java:22-23`
- 인증된 사용자 누구나 `/ranking` 호출로 전체 학번(=로그인 아이디) 목록 확보 가능. `/auth/login`에 시도 제한이 없어 크리덴셜 스터핑 위험 증가.

### NH-2. AuthController의 signup/login에 `@Valid` 누락 [신규 — 라운드 1 M-1을 흡수]
- **위치**: `AuthController.java:38-39, 48-49`
- Bean Validation이 전혀 트리거되지 않음. 같은 프로젝트의 `FavoriteController`와 패턴 불일치.

---

## Medium

### NM-1. refreshToken 쿠키(SameSite=None) + 전역 CSRF 비활성화 [신규]
- **위치**: `AuthController.java:68-77`, `SecurityConfig.java:50-51`
- 현재는 쿠키를 소비하는 엔드포인트(refresh/logout)가 없어 즉시 악용 불가. 추가되는 시점에 재확인 필요.

### M-2. mariadb-java-client 3.5.1 — 조건부 MITM 취약점(CVE-2026-55856 등) [라운드 1 유지]
- `build.gradle` 변경 없음. 라운드 1 dependency-audit 내용 그대로 유효.

### M-3. 인증/인가 실패(401/403) 응답 포맷 불일치 [라운드 1 유지]
- `SecurityConfig.java`에 커스텀 `AuthenticationEntryPoint`/`AccessDeniedHandler` 여전히 없음. H-3(필터 예외 처리)와는 별개 지점.

### M-4. SignupResponse.id 항상 null [라운드 1 유지]
- `SignupService.java` 변경 없음(C-1 puuid 버그 포함 그대로). 코드가 컴파일되면(puuid 이슈 해결 후) 바로 드러날 문제.

### NM-2. LoginService.login()에 @Transactional 없음 [신규]
- **위치**: `LoginService.java:34-77`
- 동일 커밋의 `FavoriteService`/`RankingService`와 패턴 불일치.

---

## Low (모두 라운드 1과 동일, 코드 변경 없음 — 유지)

- L-1. JWT `iss` 클레임 발급하지만 검증 시 강제하지 않음
- L-2. `CustomUserDetails.getAuthorities()` 항상 빈 권한 목록
- L-3. 비밀번호 길이 제약 DTO(`min=2`) vs 서비스 로직(`min 6`) 불일치
- L-4. Spring Security 7.1.0의 CVE-2026-59270(LDAP) — 기능 미사용, 참고용
- L-5. jjwt 0.12.6 legacy 버전 표시
- L-6. JwtAuthenticationFilter의 `jwtTokenProvider != null` 무의미한 방어 코드
- L-7. 온보딩/배포용 예시 설정 파일 부재

---

## 평가 보류 (변경 없음)
- Access/Refresh Token 회전·재사용 탐지 — `/auth/refresh`, `/auth/logout` 엔드포인트 여전히 없음
- HTTPS/HSTS 실제 적용 — 배포 인프라 의존
