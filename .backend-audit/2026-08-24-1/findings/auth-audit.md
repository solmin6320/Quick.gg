# auth-audit 결과

총평: Spring Security 설정 자체(필터 순서, CSRF, 세션 정책, 비밀번호 해싱)는 JWT 기반 stateless API로서 기본기는 올바르게 갖춰져 있다. 다만 프로젝트가 초기 단계라 로그인/인가(RBAC) 관련 실제 엔드포인트가 아직 구현되지 않았고, 회원가입 로직에 있는 "학교 인증코드" 검증이 값만 받고 실제 검사를 하지 않는 상태다.

---

### [카테고리: 보안 / High] 회원가입 시 학교 인증코드(verificationCode) 실제 검증 로직 없음

- **위치**: `src/main/java/com/example/quick_gg/service/SignupService.java:23-71`, `src/main/java/com/example/quick_gg/dto/request/SignupRequest.java:38-40`
- **문제**: `SignupRequest`에 `verificationCode` 필드가 있고 `@NotBlank`로 "값이 비어있지 않은지"만 검증한다. 그러나 `SignupService.signup()` 어디에서도 이 값을 실제 인증코드 목록/규칙과 비교하는 로직이 없다. 이 필드가 "학교 소속 학생만 가입 가능"하도록 만드는 접근 제어 게이트라면, 현재는 아무 문자열이나 넣어도 통과되므로 사실상 그 게이트가 없는 것과 동일하다.
- **코드 근거**:
  ```java
  // SignupRequest.java
  @NotBlank(message = "학교 인증코드를 입력해주세요")
  private String verificationCode;

  // SignupService.java signup() 메서드 - verificationCode 를 사용하는 코드가 전혀 없음
  public SignupResponse signup(SignupRequest request) {
      if (repository.existsByStudentNumber(request.getStudentID())) { ... }
      if (!request.getPassword().equals(request.getConfirmPassword())) { ... }
      // ... verificationCode 검증 없이 바로 저장 진행
  }
  ```
- **판단 근거(왜 위험한지)**: OWASP ASVS 5.0의 접근 제어 원칙(모든 접근 제어 결정은 서버 측에서 강제되어야 하며, 클라이언트가 제출한 값이 존재한다는 사실만으로 검증이 완료된 것으로 취급해서는 안 됨)에 비추어, 값의 "형식적 존재"와 "내용의 유효성 검증"은 다른 문제다. 현재 코드는 전자만 하고 있다.

### [카테고리: 보안 / Medium] JwtAuthenticationFilter에서 인증 처리 중 발생 가능한 예외가 처리되지 않음

- **위치**: `src/main/java/com/example/quick_gg/security/JwtAuthenticationFilter.java:38-61`
- **문제**: `userDetailsService.loadUserByUsername(studentID)`는 사용자가 DB에 없으면 `UsernameNotFoundException`(RuntimeException)을 던진다. 토큰 발급 이후 해당 계정이 삭제되었거나 다른 이유로 조회에 실패하면 이 예외가 필터 내부에서 잡히지 않고 그대로 전파된다. 서블릿 필터 단계는 `DispatcherServlet` 이전이라 `@RestControllerAdvice`(`GlobalExceptionHandler`)가 이를 가로채지 못하고, WAS/Spring Boot 기본 에러 처리 경로로 빠져 일관된 JSON 에러 응답 대신 기본 에러 페이지나 스택트레이스 노출 위험이 있다.
- **코드 근거**:
  ```java
  if (jwtTokenProvider != null && jwtTokenProvider.validateToken(token)) {
      Claims claims = jwtTokenProvider.parseClaims(token);
      String studentID = claims.getSubject();
      UserDetails userDetails = userDetailsService.loadUserByUsername(studentID); // 예외 미처리
      ...
  }
  ```
- **판단 근거(왜 위험한지)**: Spring Security 공식 문서는 커스텀 필터에서 발생하는 예외를 `ExceptionTranslationFilter`가 다루지 못하는 경우(인증 필터가 그 이후에 위치) 직접 try-catch로 처리하도록 권고한다. 정보 노출 관점에서는 OWASP "Improper Error Handling" 항목에 해당. (이 항목은 `exception-audit`의 "필터 예외 처리 구조" 점검과 근본 원인이 동일하므로 master-list에서 병합 예정)

### [카테고리: 보안 / Info] 문제 없음 — CSRF 비활성화

- **위치**: `SecurityConfig.java:44`
- Stateless JWT 인증만 사용하고 세션 쿠키 기반 인증을 사용하지 않으므로 CSRF 비활성화는 적절하다 (OWASP CSRF Prevention Cheat Sheet: 쿠키 기반 세션을 쓰지 않는 토큰 기반 API는 CSRF 보호가 불필요).

### [카테고리: 보안 / Info] 문제 없음 — 인가 규칙 순서

- **위치**: `SecurityConfig.java:51-55`
- `/auth/signup`, `/auth/login`에 대한 `permitAll()`이 `anyRequest().authenticated()`보다 먼저 선언되어 있어 Spring Security의 "먼저 매치되는 규칙 우선" 원칙에 맞게 올바르게 동작한다.

### [카테고리: 보안 / Low] getAuthorities()가 항상 빈 권한 목록 반환

- **위치**: `src/main/java/com/example/quick_gg/security/CustomUserDetails.java:34-37`
- **문제**: 현재는 역할 구분(관리자/학생 등)이 프로젝트에 존재하지 않아 당장 취약점은 아니지만, 향후 관리자 기능이나 역할 기반 접근 제어(`@PreAuthorize` 등)가 추가될 때 이 지점부터 다시 작업해야 한다는 점을 기록해 둔다.

### [카테고리: 보안 / Info] 평가 보류 — 로그인 엔드포인트 미구현

- `LoginRequest`/`LoginResponse` DTO는 존재하지만 이를 사용하는 Controller/Service가 아직 없다. 무차별 대입 공격 방어, user enumeration 방지, 로그인 실패 응답 일관성 등은 실제 로그인 로직이 작성된 후 재점검이 필요하다.

### [카테고리: 보안 / Info] 문제 없음 — 비밀번호 해싱

- **위치**: `SecurityConfig.java:26-29`, `SignupService.java:45`
- `BCryptPasswordEncoder`로 해싱 후 저장하며 평문 비밀번호를 DB에 저장하지 않는다.

### [카테고리: 보안 / Info] 문제 없음 — IDOR / 소유자 검증

- 현재 리소스를 파라미터로 조회하는 Controller 자체가 없어 점검 대상이 없다. Controller가 추가되는 시점에 재점검 필요.

### [카테고리: 보안 / Info] 문제 없음 — 하드코딩된 기본/관리자 계정

- 소스 및 (존재하지 않는) 설정 파일 전체에서 하드코딩된 계정 정보를 발견하지 못했다.
