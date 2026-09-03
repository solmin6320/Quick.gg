# 프로젝트 분석 결과

## 기본 정보
- 빌드 도구: Gradle
- Spring Boot 버전: 4.1.0 (매우 최신/사전 릴리즈 계열 — 실제 존재하는 버전인지 dependency-audit에서 재확인 필요)
- Java 버전: 21 (toolchain)
- 패키징 방식: jar (기본, war 플러그인 없음)
- DB: MariaDB + Flyway 마이그레이션 (V1~V4)

## 구조 요약
- Controller: **0개** — `@RestController`/`@Controller` 없음. 아직 API 엔드포인트가 노출되지 않은 초기 단계 프로젝트로 보임.
- Service: 1개 (`SignupService`)
- Repository: 2개, JPA 기반 (`RefreshTokenRepository`, `StudentRepository`)
- Entity: 2개 (`StudentEntity`, `RefreshTokenEntity`)
- Security 설정: `config/SecurityConfig.java`, `security/CustomUserDetails.java`, `security/CustomUserDetailsService.java`, `security/JwtAuthenticationFilter.java`
- JWT: `jwt/JwtProperties.java`, `jwt/JwtTokenProvider.java`
- 전역 예외 처리: `exception/GlobalExceptionHandler.java` 존재 (+ `CustomException`, `ErrorCode`)
- 설정 파일: **`application.yml`/`application.properties`가 프로젝트 전체에 하나도 없음** — DB 접속 정보, JWT 시크릿 등 필수 설정값의 소스를 config-audit/secret-audit 단계에서 반드시 확인 필요 (환경변수만 쓰는지, 아니면 아예 누락된 것인지)
- 배포 관련 파일: Dockerfile/docker-compose/k8s 매니페스트 없음
- 테스트 코드: `src/test/java`에 기본 생성된 `ApplicationTests.java` 1개뿐, 실질적 단위/통합 테스트 없음

## 탐지된 시그널
- JWT 사용: 예 (`jjwt` 의존성, `JwtTokenProvider`, `JwtAuthenticationFilter`)
- 파일 업로드(MultipartFile): 아니오
- 외부 API 연동: `webflux` 의존성 존재 (build.gradle 주석: "Riot API 호출용 WebClient") — 아니오 (현재 코드상 WebClient 사용처 없음, 향후 예정)
- 비동기/멀티스레드(@Async 등): 아니오
- 캐시/세션 저장소(Redis 등): 아니오
- Refresh Token: 예 (`RefreshTokenEntity`, `RefreshTokenRepository`) — 다중 로그인 기기 관련 트레이드오프가 커밋 로그에 언급됨 → jwt-audit에서 중점 확인

## 적용 대상 전문 스킬

### 보안
- [x] auth-audit — Spring Security 설정(`SecurityConfig`, `CustomUserDetailsService`, `JwtAuthenticationFilter`) 존재
- [x] jwt-audit — JWT 발급/검증/RefreshToken 로직 존재
- [x] input-validation-audit — `SignupRequest`/`LoginRequest` 등 사용자 입력 DTO 및 JPA 기반 DB 접근 존재
- [x] config-audit — SecurityConfig(CORS/CSRF 등) 점검 필요, 단 application.yml/properties 자체가 없다는 이례적 상황을 최우선으로 확인
- [x] dependency-audit — Spring Boot 4.1.0 등 버전 실재 여부/EOL 여부 확인 필요
- [x] secret-audit — JwtProperties가 시크릿을 어디서 읽는지(하드코딩 여부) 확인 필요

### 에러
- [x] error-audit — 항상 포함. 동시성 시그널 없음 → 가중치 없이 표준 체크리스트 적용

### 예외
- [x] exception-audit — `GlobalExceptionHandler` 존재하므로 커버리지/구조 위주로 점검 (완전 누락 케이스 아님)
