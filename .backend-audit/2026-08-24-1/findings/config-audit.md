# config-audit 결과

총평: `application.yml`/`application.properties`를 포함한 설정 파일이 리포지토리에 전혀 없어, DB 접속 정보·JWT 시크릿 등 필수 설정값의 출처를 확인할 수 없다(가장 심각한 항목). 반면 CORS·Actuator·Swagger처럼 "과도하게 열어서 문제가 되는" 항목들은 애초에 관련 설정/의존성 자체가 없어 현재 시점 기준으로는 노출 위험이 없다.

---

### [카테고리: 보안 / Critical] 설정 파일 완전 부재 — DB/JWT 등 필수 설정값의 출처를 알 수 없음

- **위치**: 프로젝트 전체 (`src/main/resources/` 하위에 `application.yml`/`application.properties`가 존재하지 않음)
- **문제**: `spring-boot-starter-data-jpa` + MariaDB 드라이버를 사용하지만 `spring.datasource.*` 설정이 어디에도 없고, `JwtProperties`가 요구하는 `jwt.secret`/`jwt.issuer`/`jwt.*Expiration` 값도 어디에도 없다. `.env` 파일, `docker-compose.yml`, README 등 외부 주입 방식을 암시하는 어떤 흔적도 리포지토리에 없다. 이 상태로는 로컬 실행/배포 시 필수 값이 어디서 오는지 코드만 보고는 알 수 없다.
- **코드 근거**:
  ```
  find src/main/resources -type f
  → db/migration/*.sql 만 존재, application.yml/properties 없음
  ```
- **판단 근거(왜 위험한지)**: Spring Boot 공식 문서상 `@ConfigurationProperties`로 선언된 값은 외부 설정 소스(`application.yml`, 환경변수, `--` 커맨드라인 인자 등) 중 하나에서 반드시 주입되어야 하며, 없으면 기본값 `null`이 유지된다. 이는 보안 취약점이라기보다 "설정 관리 방식이 불투명함"에 대한 Critical 등급 지적이다 — 시크릿을 하드코딩하지 않은 것은 바람직하지만, 실제로 무엇을 어디서 채워야 하는지에 대한 근거가 전무해 배포 시 오설정(예: 개발용 임시 값이 그대로 운영에 사용됨) 위험이 오히려 커진다. (`jwt-audit`의 관련 항목과 근본 원인 동일 — master-list에서 병합)

### [카테고리: 보안 / Info] 문제 없음 — CORS

- 코드베이스 전체에 `CorsConfiguration`, `allowedOrigins`, `@CrossOrigin` 등 CORS 관련 설정이 전혀 없다. 과도하게 허용된 CORS 설정으로 인한 위험은 없으나, 프론트엔드 연동 시점에 명시적으로 설정이 필요하다는 점은 기능적으로 남아있는 작업이다(보안 취약점 아님).

### [카테고리: 보안 / Info] 해당 없음 — 쿠키 속성

- 인증에 쿠키를 전혀 사용하지 않고 `Authorization: Bearer` 헤더 기반 JWT만 사용한다(`JwtAuthenticationFilter.java:67-76`). `HttpOnly`/`Secure`/`SameSite` 점검 대상 자체가 없다.

### [카테고리: 보안 / Info] 문제 없음 — Actuator 노출

- `build.gradle`에 `spring-boot-starter-actuator` 의존성이 없다. Actuator 엔드포인트 자체가 클래스패스에 존재하지 않으므로 노출 위험이 없다.

### [카테고리: 보안 / Info] 문제 없음 (확인 범위 내) — 에러 응답의 스택트레이스 노출

- **위치**: `GlobalExceptionHandler.java:60-66`
- `@ExceptionHandler(Exception.class)`가 클라이언트에는 `ErrorCode.INTERNAL_SERVER_ERROR.getMessage()`(고정 문구)만 반환하고, 실제 예외는 `log.error`로 서버 로그에만 남긴다. `server.error.include-stacktrace` 설정이 없어도 Spring Boot 기본값(`never`)이 적용되어 화이트라벨 에러 페이지 역시 스택트레이스를 노출하지 않는다.
- 단, 이 핸들러가 커버하지 못하는 서블릿 필터 단계의 예외(`auth-audit`에서 지적한 `JwtAuthenticationFilter` 항목)는 별도로 확인이 필요하다.

### [카테고리: 보안 / Info] 확인 불가 — HTTPS 강제 및 HSTS

- **위치**: `SecurityConfig.java` (`.headers(...)` 미호출, `.requiresChannel(...)` 없음)
- Spring Security 6.x는 `.headers()`를 커스터마이징하지 않으면 `Strict-Transport-Security` 헤더를 기본 활성화하지만, 이는 HTTPS 연결에서만 의미가 있다. 이 리포지토리에는 리버스 프록시/로드밸런서 등 TLS 종료 구성이 없어(Dockerfile·인프라 코드 부재), 실제 운영 환경에서 HTTPS가 강제되는지는 애플리케이션 코드만으로 확인할 수 없다.

### [카테고리: 보안 / Info] 문제 없음 — 기본 보안 헤더 비활성화 여부

- **위치**: `SecurityConfig.java:41-65`
- `.headers(...)`를 호출해 기본값을 끄는 코드가 없으므로 Spring Security 6.x의 기본 헤더(`X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Cache-Control` 등)가 그대로 적용된다.

### [카테고리: 보안 / Info] 해당 없음 — API 문서(Swagger) 노출

- `build.gradle`에 `springdoc-openapi`/`springfox` 등 관련 의존성이 없다.

### [카테고리: 보안 / Info] 확인 불가 — 프로파일 분리(운영/개발 설정)

- 프로파일별 설정 파일(`application-dev.yml`, `application-prod.yml` 등) 자체가 없어 "운영 프로파일에 개발용 설정이 남아있는지"를 점검할 대상이 없다. 근본 원인은 위 Critical 항목(설정 파일 완전 부재)과 동일하다.
