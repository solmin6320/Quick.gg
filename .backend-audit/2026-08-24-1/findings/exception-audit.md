# exception-audit 결과

총평: `GlobalExceptionHandler`(`@RestControllerAdvice`)가 존재하며 커스텀 예외/검증 실패/예상 못한 예외까지 3단 구조로 비교적 잘 커버하고 있다. 다만 이 핸들러가 미치지 못하는 두 영역 — ① 서블릿 필터 단계에서 발생하는 예외, ② Spring Security의 인증/인가 실패 응답 — 이 있어 "일관된 JSON 에러 응답"이라는 목표가 이 두 경로에서는 깨진다. `catch` 블록 자체는 프로젝트 전체에 단 하나만 존재하며 정상적으로 처리되고 있다.

---

### [카테고리: 예외 / High] JwtAuthenticationFilter에서 발생하는 예외가 전역 예외 처리 커버리지 밖에 있음

- **위치**: `src/main/java/com/example/quick_gg/security/JwtAuthenticationFilter.java:38-61`, `src/main/java/com/example/quick_gg/exception/GlobalExceptionHandler.java` (전체)
- **문제**: `@RestControllerAdvice`는 `DispatcherServlet`이 Controller 메서드를 호출하는 과정에서 발생한 예외만 가로챈다. `JwtAuthenticationFilter`는 `DispatcherServlet` 이전에 실행되는 서블릿 필터라 그 안에서 발생하는 예외(`userDetailsService.loadUserByUsername()`이 던질 수 있는 `UsernameNotFoundException` 등)는 `GlobalExceptionHandler`가 전혀 인지하지 못한다. 결과적으로 해당 요청은 애플리케이션이 정의한 `ErrorResponse` JSON 형식이 아니라 서블릿 컨테이너(내장 Tomcat)의 기본 에러 처리 경로로 빠진다.
- **코드 근거**:
  ```java
  // JwtAuthenticationFilter.java - try-catch 없음
  UserDetails userDetails = userDetailsService.loadUserByUsername(studentID);
  ```
  ```java
  // GlobalExceptionHandler.java - @RestControllerAdvice는 Controller 계층만 커버
  @RestControllerAdvice
  public class GlobalExceptionHandler { ... }
  ```
- **판단 근거(왜 문제인지)**: Spring 공식 문서(Spring Security Reference, "Exception Handling")는 `ExceptionTranslationFilter`가 `AuthenticationException`/`AccessDeniedException`만 처리하며, 그 외 필터에서 던져진 임의의 런타임 예외는 필터 체인을 그대로 타고 올라가 서블릿 컨테이너까지 전파된다고 명시한다. 이는 `auth-audit`에서 보안(정보노출) 관점으로 지적한 항목과 근본 원인이 동일하다 — 여기서는 "예외 처리 구조의 커버리지 공백"으로 기록한다.

### [카테고리: 예외 / Medium] 인증/인가 실패(401/403) 응답이 GlobalExceptionHandler의 응답 형식과 다르게 나갈 가능성

- **위치**: `src/main/java/com/example/quick_gg/config/SecurityConfig.java` (전체 — `AuthenticationEntryPoint`/`AccessDeniedHandler` 커스텀 설정 없음)
- **문제**: `anyRequest().authenticated()`에 걸려 인증되지 않은 요청이 들어오면 Spring Security가 기본 `AuthenticationEntryPoint`로 응답을 생성한다. 이 프로젝트는 커스텀 `AuthenticationEntryPoint`/`AccessDeniedHandler`를 등록하지 않았으므로, 이 401/403 응답은 `GlobalExceptionHandler`가 만드는 `ErrorResponse`(message + timestamp) 형식이 아니라 Spring Security 기본 형식으로 나간다. 즉 클라이언트 입장에서 "검증 실패는 A 형식, 인증 실패는 B 형식"으로 에러 응답 스키마가 두 가지가 된다.
- **판단 근거(왜 문제인지)**: 일반적인 API 설계 원칙상 모든 에러 응답은 동일한 스키마를 따르는 것이 클라이언트 구현을 단순하게 만든다. Spring Security 공식 문서는 `.exceptionHandling(ex -> ex.authenticationEntryPoint(...).accessDeniedHandler(...))`로 커스텀 응답을 만들 것을 안내하는데, 현재 `SecurityConfig`에는 이 설정이 없다.

### [카테고리: 예외 / Info] 문제 없음 — 광범위 catch/예외 삼킴

- **위치**: 프로젝트 전체 (`catch` 블록은 `JwtTokenProvider.java:100-106` 단 한 곳)
- 유일한 `catch` 블록은 JWT 파싱/검증 실패를 의도적으로 "검증 실패(false)"로 변환하는 정상적인 fail-closed 패턴이며, 예외를 삼키고 마치 성공한 것처럼 진행하는 코드가 아니다. `GlobalExceptionHandler`의 `catch (Exception e)`도 마지막 안전망으로 로그를 남기고 500을 반환하는 의도된 설계이며, 이후 로직이 성공한 것처럼 이어지지 않는다.

### [카테고리: 예외 / Info] 문제 없음 — 예외 계층 설계

- **위치**: `src/main/java/com/example/quick_gg/exception/CustomException.java`, `ErrorCode.java`
- 비즈니스 예외를 `RuntimeException`을 직접 던지지 않고 `CustomException` + `ErrorCode` enum으로 감싸 HTTP 상태코드와 메시지를 일관되게 관리하고 있다.

### [카테고리: 예외 / Info] 평가 보류 — 트랜잭션과 예외(롤백 규칙)

- 프로젝트 전체에 `@Transactional`이 하나도 없다(`Grep` 결과 무매칭). 현재 `SignupService.signup()`은 단일 `save()` 호출만 있어 부분 실패로 인한 데이터 불일치 위험은 낮지만, 여러 단계의 쓰기 작업이 추가되는 시점에는 `@Transactional`과 `rollbackFor` 정책을 재점검해야 한다. (관련 로직 문제는 `error-audit`의 TOCTOU 항목 참고)

### [카테고리: 예외 / Info] 해당 없음 — 외부 연동 타임아웃/재시도, 비동기 예외 처리

- `RestTemplate`/`WebClient`/`@Retryable`/`@Async` 사용 코드가 현재 프로젝트에 없어(`webflux` 의존성은 선언되어 있으나 실제 사용처 없음) 점검 대상이 없다. Riot API 연동이 실제로 구현되는 시점에 타임아웃·재시도 정책을 재점검해야 한다.
