# jwt-audit 결과

총평: JWT 서명/검증 자체(`JwtTokenProvider`)는 jjwt 0.12.x의 최신 API(`verifyWith`, `Keys.hmacShaKeyFor`)를 사용해 알고리즘 혼동·약한 키 문제를 라이브러리 차원에서 방지하고 있어 구조적으로는 양호하다. 다만 로그인 컨트롤러/서비스가 아직 구현되지 않아 `createAccessToken`/`createRefreshToken`이 실제로 호출되는 지점이 코드베이스 어디에도 없으며, Refresh Token 재사용 탐지·회전·로그아웃 무효화 등 설계 항목 대부분은 "평가 보류" 상태다.

---

### [카테고리: 보안 / Info] 문제 없음 — 서명 알고리즘 및 alg 혼동 방지

- **위치**: `src/main/java/com/example/quick_gg/jwt/JwtTokenProvider.java:83-107`
- `Jwts.parser().verifyWith(secretKey)`는 대칭키(SecretKey) 기반 검증만 허용하도록 강제되어, 토큰 헤더의 `alg`를 그대로 신뢰해 RS256/HS256을 혼동하거나 `alg: none`을 허용하는 경로가 없다.
- **판단 근거**: jjwt 0.12.x 공식 문서상 `verifyWith(Key)`는 키 타입과 호환되는 서명군만 검증에 사용하도록 설계되어 있어, "algorithm confusion" 공격 벡터(RFC 8725 §3.1, JWT BCP)를 원천적으로 차단한다.

### [카테고리: 보안 / Info] 문제 없음 — 서명 키 하드코딩 여부

- **위치**: `src/main/java/com/example/quick_gg/jwt/JwtProperties.java`
- `@ConfigurationProperties(prefix = "jwt")`로 외부 설정에서 주입받으며 소스코드 내 하드코딩된 값은 없다. (단, 실제 설정값 자체가 프로젝트 어디에도 존재하지 않는 문제는 별도 항목 및 `secret-audit`/`config-audit`에서 다룸)

### [카테고리: 보안 / Medium] 서명 키 값이 프로젝트 어디에도 설정되어 있지 않음 (확인 불가)

- **위치**: `application.yml`/`application.properties` 부재, `JwtProperties.java:12-17`, `JwtTokenProvider.java:26-30`
- **문제**: `jwt.secret`을 비롯한 모든 JWT 설정값(issuer, accessTokenExpiration, refreshTokenExpiration)이 리포지토리 내 어떤 파일에도 정의되어 있지 않다. 외부 환경변수로 주입하는 방식일 수도 있으나 그 증거(예: `.env.example`, README, docker-compose)도 없어 확인이 불가능하다. 값이 주입되지 않으면 `getSecret()`이 `null`을 반환하고 `@PostConstruct`의 `Keys.hmacShaKeyFor(null.getBytes(...))`에서 `NullPointerException`이 발생해 애플리케이션이 기동 자체를 못 한다.
- **코드 근거**:
  ```java
  @PostConstruct
  public void init() {
      this.secretKey = Keys.hmacShaKeyFor(
              jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8) // secret == null 이면 NPE
      );
  }
  ```
- **판단 근거(왜 위험한지)**: 일반적인 JWT 보안 원칙상, 서명키를 소스코드 밖으로 뺀 것 자체는 OWASP가 권장하는 올바른 방향이다. 다만 "빠졌을 때 안전하게 실패하는지"와 별개로, 현재는 실제 배포/실행 시 필요한 설정값의 소스(어디서 어떻게 주입되는지)를 리포지토리만 봐서는 전혀 알 수 없어 감사 범위에서 "확인 불가"로 남긴다. 실제 키 길이(HS256 최소 256비트, RFC 7518 §3.2)를 만족하는지도 같은 이유로 확인 불가하다 — 다만 값이 존재한다면 `Keys.hmacShaKeyFor`가 최소 길이 미만일 때 `WeakKeyException`을 던져 fail-fast 하므로(jjwt 공식 Javadoc), 약한 키가 조용히 사용될 가능성은 낮다.

### [카테고리: 보안 / Low] 발급 시 설정한 issuer(iss) 클레임이 검증 시 강제되지 않음

- **위치**: `JwtTokenProvider.java:83-89` (`parseClaims`), `92-107` (`validateToken`)
- **문제**: 토큰 생성 시 `.issuer(jwtProperties.getIssuer())`로 `iss` 클레임을 넣지만, 검증 로직(`parseClaims`, `validateToken`)에는 `.requireIssuer(...)` 같은 강제 검증이 없다. 즉 서명만 유효하면 `iss` 값과 무관하게 통과된다.
- **판단 근거**: 현재는 이 서비스만 토큰을 발급/검증하는 단일 발급자 구조라 즉시 악용 가능한 시나리오는 제한적이지만, RFC 7519 §4.1.1은 `iss`를 명시했다면 검증측이 이를 확인할 것을 전제로 설계되었다. 클레임만 심고 검증하지 않는 것은 "선언은 했지만 강제하지 않는" 불일치 상태다.

### [카테고리: 보안 / Info] 문제 없음 (해당 없음) — audience(aud) 클레임

- 발급/검증 어느 쪽에서도 `aud`를 사용하지 않는다. 단일 서비스·단일 클라이언트 구조에서는 필수가 아니며, 추후 여러 클라이언트(모바일/웹)나 서비스가 토큰을 공유하게 되면 도입을 고려할 항목으로 기록만 해 둔다.

### [카테고리: 보안 / Info] 평가 보류 — Access/Refresh Token 발급·회전·재사용 탐지 흐름

- **위치**: `JwtTokenProvider.java:38-80`, `RefreshTokenRepository.java`, `RefreshTokenEntity.java`
- `createAccessToken`/`createRefreshToken`을 호출하는 로그인 서비스/컨트롤러가 코드베이스에 아직 없다 (`Grep` 결과 두 메서드 모두 정의부 외 호출부 없음). 마찬가지로 `RefreshTokenRepository.save(...)`를 호출하는 코드도 없어, Refresh Token이 실제로 DB에 저장되는 지점 자체가 아직 구현되지 않았다.
- 따라서 다음 항목은 실제 구현이 나온 뒤 재점검이 필요한 "평가 보류" 상태다: Access Token 만료 시간의 적절성(실제 값 미설정), Refresh Token Rotation, 재사용 탐지, 로그아웃/비밀번호 변경 시 토큰 무효화. `RefreshTokenEntity`에 `revoked` 컬럼이 있고 `RefreshTokenRepository`에 `deleteByToken`/`deleteByStudent`가 정의되어 있어 무효화를 위한 기반은 마련되어 있다는 점은 긍정적이다.

### [카테고리: 보안 / Info] 해당 없음 — 클라이언트 저장/전송 방식

- 이 리포지토리는 백엔드 전용이며 프론트엔드 코드가 없어 `localStorage` vs `httpOnly` 쿠키 저장 방식은 점검 대상이 아니다. 다만 `JwtAuthenticationFilter`가 `Authorization: Bearer` 헤더만 읽도록 구현되어 있어(쿠키 미사용), 프론트엔드가 어떻게 구현되든 최소한 서버 쪽에서 쿼리 파라미터 토큰 전달을 유도하지는 않는다.

### [카테고리: 보안 / Info] 문제 없음 — 클레임 내 민감정보 포함 여부

- **위치**: `JwtTokenProvider.java:50-57`, `72-79`
- Access/Refresh Token의 클레임은 `subject`(학번), `issuedAt`, `expiration`, `issuer`, `jti`뿐이며 비밀번호 등 민감정보는 포함되지 않는다.
