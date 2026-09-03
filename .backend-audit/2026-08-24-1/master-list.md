# 통합 이슈 리스트 (master-list)

집계 대상: `auth-audit`, `jwt-audit`, `input-validation-audit`, `config-audit`, `dependency-audit`, `secret-audit`, `error-audit`, `exception-audit` (8개 스킬 모두 실행 완료, 실패 없음)

## 요약

- **실제 조치가 필요한 항목**: 16건 (Critical 2 / High 3 / Medium 4 / Low 7)
- **평가 보류(현재는 미구현 기능이라 판단 불가, 추후 재점검 필요)**: 5건
- **점검했으나 문제 없음/해당 없음**: 약 25건 (각 findings/*.md 참고)
- **카테고리별**: 보안 11건, 에러 4건, 예외 2건 (일부 항목은 2개 이상 스킬이 같은 근본 원인을 지적해 병합)

이 프로젝트는 회원가입 로직을 막 작성한 초기 단계(최근 커밋: "회훤가입 DTO, 요청, 응답 @Builder 추가 및 회원가입 로직 작성")이며, 컨트롤러·로그인 로직이 아직 없다. 그래서 발견된 이슈 중 상당수가 "이미 있는 코드의 결함"이 아니라 "아직 완성되지 않은 부분"의 성격을 띤다 — 항목별 대화에서 의도/미완성 여부를 확인해야 한다.

---

## Critical

### C-1. SignupService.java 컴파일 실패 — `puuid` 미정의 변수 참조
- **관련 스킬**: error-audit
- **위치**: `src/main/java/com/example/quick_gg/service/SignupService.java:53`
- `./gradlew compileJava` 실행 결과 실제로 빌드가 실패함을 확인. 현재 상태로는 애플리케이션을 실행할 수 없음.

### C-2. 설정 파일(application.yml) 부재로 DB/JWT 등 필수 설정값을 리포지토리에서 확인할 수 없음
- **관련 스킬**: config-audit, jwt-audit, secret-audit
- **위치**: `src/main/resources/` (설정 파일 없음), `.gitignore:26`
- `application.yml`/`application.properties`가 프로젝트 어디에도 없음. 단, `secret-audit`가 `.gitignore`에서 `application.yml` 제외 규칙을 발견 — 로컬에는 파일이 있고 의도적으로 git에서 제외했을 가능성이 높음(git 히스토리 전체 확인 결과 과거에도 커밋된 적 없음, 즉 유출 흔적은 없음). **이것이 "실수로 빠뜨린 것"인지 "의도된 시크릿 관리 방식"인지 사용자 확인이 반드시 필요** — 후자라면 Critical이 아니라 "온보딩 문서화 미비" 수준의 낮은 심각도로 재조정될 사안.

---

## High

### H-1. 회원가입 시 학교 인증코드(verificationCode) 실제 검증 로직 없음
- **관련 스킬**: auth-audit
- **위치**: `SignupRequest.java:38-40`, `SignupService.java:23-71`
- 값이 비어있지 않은지만 확인하고, 실제 인증코드 유효성을 검사하는 로직이 없음.

### H-2. 회원가입 중복 검사에 TOCTOU 경쟁 조건 — 동시 요청 시 409 대신 500 응답
- **관련 스킬**: error-audit
- **위치**: `SignupService.java:23-59`
- 동시에 같은 학번/소환사정보로 가입 요청이 오면 DB 유니크 제약으로 데이터 중복은 막히지만, 의도한 409 대신 처리되지 않은 예외로 500이 응답됨.

### H-3. JwtAuthenticationFilter의 예외가 전역 예외 처리기 커버리지 밖에 있음
- **관련 스킬**: exception-audit, auth-audit
- **위치**: `JwtAuthenticationFilter.java:38-61`
- 필터 단계 예외(`UsernameNotFoundException` 등)가 `GlobalExceptionHandler`를 우회해 일관되지 않은 에러 응답이나 정보 노출로 이어질 수 있음.

---

## Medium

### M-1. DTO 필드 길이 제약이 Entity DB 컬럼 길이와 불일치
- **관련 스킬**: input-validation-audit
- **위치**: `SignupRequest.java`, `StudentEntity.java`
- 상한 길이를 초과하는 입력이 Bean Validation을 통과해 DB 예외(500)로 이어질 수 있음.

### M-2. mariadb-java-client 3.5.1 — 조건부 MITM 취약점(CVE-2026-55856 등)
- **관련 스킬**: dependency-audit
- 특정 TLS 설정(`sslMode=verify-full/verify-ca` + 미고정 인증서) 조건에서만 악용 가능. 실제 조건 충족 여부는 설정 파일이 없어 확인 불가. 패치 버전 3.5.9 존재.

### M-3. 인증/인가 실패(401/403) 응답 포맷이 나머지 에러 응답과 불일치
- **관련 스킬**: exception-audit
- 커스텀 `AuthenticationEntryPoint`/`AccessDeniedHandler`가 없어 `ErrorResponse` 형식과 다른 응답이 나감.

### M-4. SignupResponse의 `id` 필드가 항상 null로 반환됨
- **관련 스킬**: error-audit
- 응답 빌더에 `.id(student.getId())` 호출이 빠져 있음.

---

## Low

### L-1. JWT `iss` 클레임을 발급 시 넣지만 검증 시 강제하지 않음 — [jwt-audit]
### L-2. `CustomUserDetails.getAuthorities()`가 항상 빈 권한 목록 반환 — [auth-audit] (향후 RBAC 확장 시 재작업 필요)
### L-3. 비밀번호 길이 제약이 DTO(`@Size min=2`)와 서비스 로직(`min 6`) 간 불일치 — [input-validation-audit]
### L-4. Spring Security 7.1.0의 최근 CVE-2026-59270(임베디드 LDAP) — [dependency-audit] (해당 기능 미사용으로 위험 낮음, 참고용)
### L-5. jjwt 0.12.6 — legacy 버전 표시, 직접 CVE는 없음 — [dependency-audit]
### L-6. `JwtAuthenticationFilter`의 `jwtTokenProvider != null` 검사가 항상 참인 무의미한 방어 코드 — [error-audit]
### L-7. 온보딩/배포용 예시 설정 파일(`application.yml.example` 등) 부재 — [secret-audit]

---

## 평가 보류 (현재 미구현 기능 — 구현 시점에 재점검 필요, 지금은 결정 불필요)

- 로그인 컨트롤러/서비스 자체가 없어 무차별 대입 방어·user enumeration 등 로그인 관련 보안 항목 [auth-audit]
- Access/Refresh Token 발급·회전·재사용 탐지 흐름 — 실제 호출부가 코드에 없음 [jwt-audit]
- `@Valid` 실제 적용 여부 — Controller 없음 [input-validation-audit]
- 트랜잭션 롤백 규칙 — `@Transactional` 전무, 현재는 단일 쓰기라 영향 낮음 [exception-audit]
- HTTPS/HSTS 실제 적용 여부 — 배포 인프라(리버스 프록시 등) 의존이라 리포지토리만으로 확인 불가 [config-audit]

---

## 문제 없음으로 확인된 주요 항목 (참고)

CSRF 비활성화(stateless 구조상 적절), 인가 규칙 순서, BCrypt 비밀번호 해싱, SQL Injection(전부 파생 쿼리), 파일 업로드/Command Injection/XXE(해당 코드 없음), 하드코딩된 시크릿 없음, Actuator/Swagger 미노출, 기본 보안 헤더 비활성화 안 됨, Spring Boot 4.1.0/Spring Framework 7.0.8/Tomcat 11.0.22/Jackson 최신 CVE 미해당 등. 상세는 각 `findings/*.md` 참고.
