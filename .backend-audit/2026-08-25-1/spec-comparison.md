# 기획서 대조 결과 (로그인 · 회원가입 담당 범위)

- 대상 브랜치: `feature/auth`
- 대상 범위: 기능 명세서 #6 회원가입, #7 로그인 및 관련 Auth API / DB / 예외 처리
- 대조 일자: 2026-08-25

## 진행 현황 요약

총 요구사항 22건 중:

| 상태 | 건수 |
|---|---|
| 완전 일치 | 10 |
| 부분 구현 | 5 |
| 불일치 | 4 |
| 미구현 | 3 |
| 문서에 없음(확인 필요) | 3 |

> **선행 차단 이슈**: 현재 `./gradlew compileJava` 가 실패한다 (`SignupService.java:53` 심볼 `puuid` 없음).
> 즉 회원가입 경로는 아직 실행 자체가 불가능하며, 아래 "완전 일치" 항목들도 런타임 검증은 되지 않은 상태다.

---

## 상세 대조

### [명세 #6] 회원가입 입력 항목 (학번/이름/비밀번호/비밀번호확인/소환사명/태그/인증코드)
- **기획서 내용**: 7개 입력 항목을 받는다.
- **코드 상태**: 완전 일치
- **근거**: `src/main/java/com/example/quick_gg/dto/request/SignupRequest.java:15-40` — `studentID`, `name`, `password`, `confirmPassword`, `summonerName`, `tag`, `verificationCode` 7개 필드 모두 존재.

### [명세 #6] 비밀번호 BCryptPasswordEncoder 해싱 저장
- **기획서 내용**: "비밀번호는 `BCryptPasswordEncoder`로 해싱하여 저장"
- **코드 상태**: 완전 일치
- **근거**: `config/SecurityConfig.java:31-34` 에서 `BCryptPasswordEncoder` 빈 등록, `service/SignupService.java:45` 에서 `passwordEncoder.encode()` 후 저장.

### [명세 #6] 학교 인증 코드 검증
- **기획서 내용**: "학교 인증 코드" 입력, 예외 처리 항목에 "인증코드 검증" 명시, API 명세 예외에 "인증코드 오류" 명시.
- **코드 상태**: **미구현**
- **근거**: `SignupRequest.java:40` 에 `verificationCode` 필드는 있으나, `SignupService.signup()` (`service/SignupService.java:23-71`) 전체에서 `getVerificationCode()` 호출이 전혀 없다. 코드 정답값을 담은 설정/상수/테이블도 존재하지 않음.
- **차이 설명**: 인증코드를 아무 값이나 넣어도 가입이 통과한다. "성일정보고 학생만 가입 가능"이라는 #6의 핵심 제약이 현재 전혀 걸려 있지 않다.

### [명세 #6 / DB] puuid 저장 (Riot API 연동)
- **기획서 내용**: `student.puuid VARCHAR(100) NOT NULL UNIQUE` — 소환사명+태그로 Riot API 조회하여 puuid를 확보해야 한다.
- **코드 상태**: **미구현 (컴파일 차단)**
- **근거**: `service/SignupService.java:53` `.puuid(puuid)` — 선언되지 않은 변수. 본인이 남긴 주석 "라이엇 API 연동 로직이 아직 완성되지 않아 컴파일 에러(고칠 예정)". Riot API 클라이언트 클래스 자체가 프로젝트에 없음 (`build.gradle:26` 에 webflux만 추가된 상태).
- **차이 설명**: 소환사 실존 검증 + puuid 확보 로직이 통째로 비어 있다. 이것이 현재 빌드를 깨뜨리는 유일한 컴파일 에러다.

### [명세 예외] 학번 중복 검사
- **코드 상태**: 완전 일치
- **근거**: `service/SignupService.java:25-27` `repository.existsByStudentNumber()` → `CONFLICT(409)`.

### [명세 예외] 소환사 중복 등록 방지
- **코드 상태**: 부분 구현
- **근거**: `service/SignupService.java:41-43` `existsBySummonerNameAndTag()` 로 (소환사명, 태그) 중복은 차단.
- **차이 설명**: DB 명세의 `puuid UNIQUE` 기준 중복 검사는 없다. `StudentRepository.java:17` 에 `existsByPuuid()` 메서드는 선언해 뒀으나 어디서도 호출하지 않는다. 소환사명을 변경한 동일 계정이 중복 등록될 수 있다.

### [명세 예외] 비밀번호 길이 검사 (6자 이상 20자 이하)
- **코드 상태**: **불일치**
- **근거**:
  - 서비스 레이어 `service/SignupService.java:35-38` — `len < 6 || len > 20` → 기획서와 일치.
  - DTO `dto/request/SignupRequest.java:23` — `@Size(min = 2, max = 20, message = "비밀번호는 2자리 이상이어야 합니다")` → 기획서(6자)와 다름.
  - `dto/request/LoginRequest.java:18` — 동일하게 `min = 2`.
- **차이 설명**: 두 곳의 기준값이 서로 다르다(2 vs 6). 현재는 `@Valid`가 없어 DTO 검증이 동작하지 않으므로 서비스 로직(6자)만 적용되지만, `@Valid`를 붙이는 순간 기준이 2자로 완화된다.

### [명세 예외] 비밀번호 확인 일치 검사
- **코드 상태**: 완전 일치
- **근거**: `service/SignupService.java:30-32` — 불일치 시 `INVALID_INPUT(400)`.

### [API] POST /auth/signup — 경로 및 201 응답
- **코드 상태**: 완전 일치
- **근거**: `controller/AuthController.java:25,37,43` — `@RequestMapping("/auth")` + `@PostMapping("/signup")`, `HttpStatus.CREATED` 반환.

### [API] POST /auth/signup — 처리 흐름 (AuthController → AuthService)
- **코드 상태**: 부분 구현 (명명 차이)
- **근거**: 기획서는 `AuthService` 단일 서비스를 상정하나, 코드는 `service/SignupService.java` 와 `service/LoginService.java` 로 분리되어 있다.
- **차이 설명**: 구조적 차이일 뿐 동작상 문제는 아니다. 기획서 표기를 코드에 맞출지 결정 필요.

### [API] Bean Validation 동작 여부
- **코드 상태**: **불일치**
- **근거**: `controller/AuthController.java:38-39`, `48-49` — `@RequestBody SignupRequest request` / `@RequestBody LoginRequest request` 에 `@Valid` 가 없다.
- **차이 설명**: `build.gradle:25` 에 validation 스타터가 있고 `exception/GlobalExceptionHandler.java:38-46` 에 `MethodArgumentNotValidException` 핸들러까지 준비돼 있는데, `@Valid` 누락으로 DTO의 `@NotBlank`/`@Size` 가 전혀 발동하지 않는다. 준비된 검증 인프라가 사실상 죽어 있는 상태.

### [명세 #7] 로그인 — 학번 + 비밀번호
- **코드 상태**: 완전 일치
- **근거**: `dto/request/LoginRequest.java:14-19`, `service/LoginService.java:37-42`.

### [명세 #7] Spring Security 인증 (AuthenticationManager)
- **코드 상태**: 완전 일치
- **근거**: `config/SecurityConfig.java:37-41` 빈 등록, `service/LoginService.java:37` `authenticationManager.authenticate(...)`, `security/CustomUserDetailsService.java:22-28` 로 학번 조회.

### [명세 #7 / API] JWT access_token 발급
- **코드 상태**: 완전 일치
- **근거**: `jwt/JwtTokenProvider.java:38-58` `createAccessToken()`, `service/LoginService.java:56`, `controller/AuthController.java:60-64` 응답 바디에 accessToken 포함.

### [API] JwtAuthenticationFilter — 요청마다 토큰 검증 후 SecurityContext 설정
- **코드 상태**: 부분 구현
- **근거**: `security/JwtAuthenticationFilter.java:28-65` 로 구현되어 있고 `config/SecurityConfig.java:65-68` 에서 `UsernamePasswordAuthenticationFilter` 앞에 등록됨.
- **차이 설명**: `JwtAuthenticationFilter.java:38` 의 조건이 `if (jwtTokenProvider != null && ...)` 로 되어 있다. 의도는 `token != null` 로 보인다. 현재는 `validateToken(null)` 이 `IllegalArgumentException` 을 잡아 `false` 를 반환해 우연히 동작하지만, 토큰 없는 모든 요청이 예외 생성 경로를 타게 된다. 검증 필요.

### [API] SessionCreationPolicy.STATELESS
- **코드 상태**: 완전 일치
- **근거**: `config/SecurityConfig.java:55-57`.

### [API] /auth/signup, /auth/login permitAll + 그 외 인증 필요
- **코드 상태**: 완전 일치
- **근거**: `config/SecurityConfig.java:59-63`.

### [DB] student 테이블
- **코드 상태**: 완전 일치
- **근거**: `src/main/resources/db/migration/ V1__create_student_table.sql` 및 `entity/StudentEntity.java:23-50` — 컬럼/제약(UNIQUE student_number, UNIQUE puuid, UNIQUE(summoner_name, tag)) 모두 명세와 동일.
- **비고(확인 필요)**: 마이그레이션 파일명 `" V1__create_student_table.sql"` 앞에 **공백**이 들어가 있다. Flyway가 버전 파싱에 실패할 가능성이 높다 (V2~V4는 정상). 파일명 수정 필요.

### [DB] refresh_token 테이블
- **코드 상태**: 완전 일치
- **근거**: `db/migration/V4__create_refresh_token_table.sql`, `entity/RefreshTokenEntity.java:14-42`.
- **비고**: `RefreshTokenEntity.java:37` `private Boolean revoked = false;` 는 `@Builder.Default` 가 없어 초기값이 무시된다는 컴파일 경고가 뜬다. 현재 `LoginService.java:66` 에서 명시적으로 `.revoked(false)` 를 넣어 주고 있어 실질 영향은 없다.

### [DB/기능] Refresh Token 활용 (재발급 / 로그아웃)
- **코드 상태**: 부분 구현
- **근거**: 발급·저장·기존 토큰 삭제는 구현됨 (`service/LoginService.java:53,57,60-70`), httpOnly 쿠키 전달도 구현됨 (`controller/AuthController.java:68-77`). `RefreshTokenRepository.java:12,15` 에 `findByToken()`, `deleteByToken()` 이 선언되어 있으나 **어디서도 호출되지 않는다**.
- **차이 설명**: `POST /auth/refresh` (토큰 재발급), `POST /auth/logout` 엔드포인트가 없다. 기획서 API 명세에도 이 두 엔드포인트는 없지만, DB 명세에 `refresh_token` 테이블(`revoked` 컬럼 포함)이 있는 이상 재발급/로그아웃 흐름은 완결되어야 한다. **기획서와 코드 중 어느 쪽을 기준으로 맞출지 확인 필요.**

### [API] 공통 응답 형식 `{ "success": true, "data": {} }` / `{ "success": false, "message": "..." }`
- **코드 상태**: **불일치**
- **근거**:
  - 성공 응답: `controller/AuthController.java:43,64` 가 `SignupResponse`/`LoginResponse` 를 그대로 반환 → `success`/`data` 래핑 없음.
  - 에러 응답: `dto/response/ErrorResponse.java:15-17` 이 `{ message, timestamp }` 형태 → `success` 필드 없음.
- **차이 설명**: 프론트가 기획서 형식(`success` 플래그 분기)을 전제로 구현 중이라면 연동 시점에 전부 깨진다. 공통 래퍼(`ApiResponse<T>`) 도입 여부 결정 필요.

### [설정] application.yml
- **코드 상태**: **미구현 (실행 차단)**
- **근거**: `src/main/resources/` 에 `db/migration/` 외 파일이 없다. `application.yml` / `application.properties` 가 프로젝트 어디에도 존재하지 않는다 (`.gitignore:22` 에 `application.yml` 이 등재되어 있어 커밋 대상에서 제외된 상태).
- **차이 설명**: `jwt/JwtProperties.java:10` 의 `@ConfigurationProperties(prefix = "jwt")` 가 요구하는 `jwt.issuer`, `jwt.secret`, `jwt.access-token-expiration`, `jwt.refresh-token-expiration` 값이 없다. 특히 `JwtTokenProvider.java:27-29` 의 `@PostConstruct init()` 에서 `jwtProperties.getSecret()` 이 null이면 애플리케이션이 기동 중 죽는다. MariaDB 데이터소스 설정도 없다.
- **확인 필요**: 로컬에만 두고 gitignore 한 것인지, 아직 안 만든 것인지. 현재 파일시스템 기준으로는 존재하지 않는다.

---

## 문서에 없음 (확인 필요)

### `SignupResponse` 에 `password` 필드 존재
- `dto/response/SignupResponse.java:21` 에 `private String password;` 가 선언되어 있다. `SignupService.java:63-69` 의 빌더에서 값을 넣지 않아 현재는 `null` 로 직렬화되지만, 회원가입 응답에 비밀번호 필드가 노출되는 형태 자체가 기획서 의도와 맞지 않아 보인다. 필드 삭제 검토 필요.

### `SignupResponse.id` 미설정
- `dto/response/SignupResponse.java:15` 의 `id` 를 `SignupService.java:63-69` 빌더에서 설정하지 않아 항상 `null` 이 내려간다. 응답에 PK를 포함할지 결정 필요.

### `StudentRepository` 쿼리 메서드 이름과 엔티티 필드명 불일치 (기동 실패 가능)
- `entity/StudentEntity.java:31` 의 필드명은 `studentID` 이고 컬럼명만 `student_number` 다. 그런데 `repository/StudentRepository.java:11,20` 은 `findByStudentNumber(...)`, `existsByStudentNumber(...)` 로 선언되어 있다. Spring Data JPA는 **컬럼명이 아니라 프로퍼티(필드)명**으로 쿼리를 파생하므로, `studentNumber` 프로퍼티를 찾지 못해 기동 시 `PropertyReferenceException` 이 발생할 가능성이 매우 높다.
- 현재 컴파일 단계에서 막혀 런타임 확인은 불가. `findByStudentID` 로 바꾸거나 엔티티 필드명을 `studentNumber` 로 바꾸는 등 정렬 필요. **`CustomUserDetailsService.java:23` 과 `LoginService.java:49` 가 모두 이 메서드에 의존하므로 로그인 전체가 영향권.**

---

## 남은 작업 정리 (우선순위)

| 순위 | 항목 | 위치 | 성격 |
|---|---|---|---|
| 1 | Riot API 연동으로 `puuid` 확보 | `service/SignupService.java:53` | 컴파일 차단 |
| 2 | `application.yml` 작성 (jwt.*, datasource) | `src/main/resources/` | 실행 차단 |
| 3 | Repository 메서드명 ↔ 엔티티 필드명 정렬 | `repository/StudentRepository.java:11,20` | 기동 실패 가능 |
| 4 | 학교 인증코드 검증 로직 | `service/SignupService.java` | 명세 미구현 |
| 5 | 마이그레이션 파일명 앞 공백 제거 | `db/migration/ V1__...sql` | 기동 실패 가능 |
| 6 | 컨트롤러 `@Valid` 추가 + 비밀번호 최소길이 6으로 통일 | `controller/AuthController.java:39,49`, `dto/request/*` | 명세 불일치 |
| 7 | 공통 응답 래퍼(`success`/`data`) 적용 여부 결정 | 전 응답 DTO | 명세 불일치 |
| 8 | puuid 기준 소환사 중복 검사 추가 | `service/SignupService.java` | 부분 구현 |
| 9 | `/auth/refresh`, `/auth/logout` 엔드포인트 | `controller/AuthController.java` | 부분 구현 (기획서 확인 필요) |
| 10 | `JwtAuthenticationFilter` null 체크 대상 수정 | `security/JwtAuthenticationFilter.java:38` | 로직 오류 |
| 11 | `SignupResponse` 의 `password` 필드 제거 / `id` 설정 | `dto/response/SignupResponse.java` | 확인 필요 |
