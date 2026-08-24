# Round 2 — 신규/변경 발견 사항 (main 브랜치 병합 반영)

조사 방법: `git diff 1202f2e main --stat`로 라운드 1 대비 실제 변경 파일을 확인하고, 신규 파일(`AuthController`, `FavoriteController`, `RankingController`, `LoginService`, `FavoriteService`, `RankingService`, `CurrentStudentProvider`, `TokenPair`, `FavoriteRequest/Response`, `RankingResponse`, `FavoriteSummonerEntity`, `SchoolRankEntity`, `Tier`)를 전부 `Read`로 직접 확인했다. 라운드 1에서 이미 사용자 결정이 난 5개 항목(C-1, C-2, H-1, H-2, H-3)은 재질문하지 않는다.

---

### [카테고리: 보안 / High] 학교 랭킹 API가 전체 학생의 로그인 아이디(학번)를 마스킹 없이 노출

- **위치**: `src/main/java/com/example/quick_gg/service/RankingService.java:108`, `src/main/java/com/example/quick_gg/dto/response/RankingResponse.java:22-23`
- **문제**: `GET /ranking`은 `anyRequest().authenticated()`로 로그인한 학생이면 누구나 호출 가능하다. 응답 필드 이름은 `maskedStudentInfo`(마스킹된 학생 정보)지만 실제 구현은 이름만 마스킹하고 학번은 그대로 이어붙인다. 이 서비스에서 학번은 로그인 아이디(username)로도 쓰인다(`LoginRequest.studentID`, `CustomUserDetails.getUsername()`). 즉 로그인한 학생 아무나 이 API 하나로 전체 재학생의 유효한 로그인 아이디 목록을 확보할 수 있다.
- **코드 근거**:
  ```java
  // RankingService.java:108
  .maskedStudentInfo(student.getStudentID() + " " + maskName(student.getName()))
  // 학번(studentID)은 마스킹되지 않고 그대로 노출됨
  ```
- **판단 근거(왜 위험한지)**: OWASP Top 10(A01:2021 - Broken Access Control, 과도한 데이터 노출)과 일반적인 인증 시스템 설계 원칙상, 로그인 아이디로 쓰이는 값은 다른 목적(순위표 등)으로 대량 노출되지 않아야 한다. 이렇게 확보한 학번 목록은 `/auth/login`에 대한 크리덴셜 스터핑/무차별 대입 공격의 사용자명 후보 리스트로 바로 활용될 수 있고, 현재 `/auth/login`에는 시도 횟수 제한이나 잠금 정책이 전혀 없다(`Grep` 결과 rate limiting 관련 코드 없음).

### [카테고리: 보안 / Medium-High] `AuthController`의 회원가입/로그인 엔드포인트에 `@Valid` 누락 — Bean Validation이 전혀 동작하지 않음

- **위치**: `src/main/java/com/example/quick_gg/controller/AuthController.java:38-39, 48-49`
- **문제**: `signup(@RequestBody SignupRequest request)`와 `login(@RequestBody LoginRequest request, ...)` 모두 `@Valid`가 없다. `SignupRequest`/`LoginRequest`에 선언된 `@NotBlank`, `@Size` 등은 `@Valid`가 있어야 Spring이 검증을 트리거하므로, 현재는 이 어노테이션들이 죽은 코드다. 같은 커밋에서 함께 작성된 `FavoriteController.addFavorite(@Valid @RequestBody FavoriteRequest request)`는 정확히 붙어 있어, 프로젝트 내에서도 일관성이 깨져 있다.
- **코드 근거**:
  ```java
  // AuthController.java - @Valid 없음
  public ResponseEntity<SignupResponse> signup(@RequestBody SignupRequest request) { ... }

  // FavoriteController.java - 올바른 패턴
  public ResponseEntity<FavoriteResponse> addFavorite(@Valid @RequestBody FavoriteRequest request) { ... }
  ```
- **판단 근거(왜 문제인지)**: Spring 공식 문서(Validation, Data Binding, and Type Conversion)상 `@RequestBody` 파라미터의 Bean Validation은 `@Valid`/`@Validated`가 있어야만 `MethodArgumentNotValidException`을 통해 트리거된다. 현재 상태에서 예를 들어 빈 문자열 `studentID`로 회원가입을 시도하면, `@NotBlank` 메시지("학번을 입력해주세요") 대신 서비스 로직(`existsByStudentNumber("")` → false)을 그대로 통과해 다른 오류(예: DB 제약 위반에 의한 500)로 이어진다. (round 1의 M-1 "DTO 길이 제약과 Entity 컬럼 불일치" 항목은 이 문제의 하위 증상이므로 이 항목에 통합해 기록한다 — `@Valid`가 붙어도 길이 상한(`@Size` max)이 없는 필드는 여전히 남는 문제라 별도 후속 조치가 필요하다.)

### [카테고리: 보안 / Medium] `refreshToken` 쿠키가 `SameSite=None`으로 발급되는데 CSRF 방어는 전역 비활성화 — 소비 엔드포인트 추가 시 위험

- **위치**: `src/main/java/com/example/quick_gg/controller/AuthController.java:68-77`, `src/main/java/com/example/quick_gg/config/SecurityConfig.java:50-51`
- **문제**: 로그인 시 `refreshToken`을 `httpOnly + Secure + SameSite=None` 쿠키로 내려준다(코드 주석에도 "다른 도메인에서 요청해도 쿠키를 실어보냄(CSRF 공격 취약)" 트레이드오프로 명시됨). 그런데 `SecurityConfig`는 CSRF를 전역적으로 비활성화한 채다. 현재는 이 쿠키를 실제로 읽어서 뭔가를 하는 엔드포인트(`/auth/refresh`, `/auth/logout` 등)가 아직 없어 즉시 악용 가능한 지점은 없지만, 그런 엔드포인트가 추가되는 순간 크로스사이트 요청이 쿠키를 실어 보낼 수 있어 CSRF에 노출된다.
- **코드 근거**:
  ```java
  ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, token)
          .httpOnly(true)
          .secure(true)
          .sameSite("None") // 트레이드오프
          .path("/")
          ...
  ```
- **판단 근거(왜 위험한지)**: OWASP CSRF Prevention Cheat Sheet는 쿠키 기반 인증을 사용하는 상태 변경 엔드포인트에는 CSRF 토큰 또는 `SameSite=Strict/Lax` 중 하나 이상의 방어가 필요하다고 명시한다. 이 프로젝트는 프론트/백엔드가 별도 도메인이라 `SameSite=Lax/Strict`를 쓸 수 없는 구조적 제약이 있고(개발자도 인지하고 있음), 그렇다면 CSRF 토큰 방식이 대안이 되어야 하는데 현재 `csrf().disable()`로 그 대안도 꺼져 있다. 지금 당장 익스플로잇 가능한 엔드포인트는 없어(`/auth/refresh`, `/auth/logout` 미구현) High가 아닌 Medium으로 판단했다.

### [카테고리: 에러 / Low-Medium] `LoginService.login()`에 `@Transactional` 없음 — 신규 서비스 간 일관성 부족

- **위치**: `src/main/java/com/example/quick_gg/service/LoginService.java:34-77`
- **문제**: 같은 커밋 묶음에서 작성된 `FavoriteService`/`RankingService`의 쓰기 메서드는 모두 `@Transactional`이 붙어 있는데, `LoginService.login()`만 없다. `refreshTokenRepository.deleteByStudent(student)`와 `refreshTokenRepository.save(refreshTokenEntity)` 사이에 원자성이 보장되지 않아, 두 호출 사이에 장애가 나면 해당 학생은 기존 리프레시 토큰도 잃고 새 토큰도 저장되지 않은 상태(강제 로그아웃과 유사한 상태)가 될 수 있다.
- **코드 근거**:
  ```java
  // LoginService.java - @Transactional 없음
  public TokenPair login(LoginRequest request) { ... }

  // FavoriteService.java - 비교 대상
  @Transactional
  public FavoriteResponse addFavorite(FavoriteRequest request) { ... }
  ```
- **판단 근거(왜 문제인지)**: Spring 트랜잭션 공식 문서상 여러 쓰기 작업의 원자성이 필요한 경우 `@Transactional`로 묶는 것이 표준 패턴이다. 발생 빈도는 낮지만(두 호출 사이 시간 창이 매우 짧음), 발생 시 사용자 영향(로그인했는데 토큰이 없어짐)이 있어 Low보다는 한 단계 위로 판단했다.
