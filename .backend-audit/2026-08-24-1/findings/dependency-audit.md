# dependency-audit 결과

조사 방법: `./gradlew dependencies --configuration compileClasspath` / `runtimeClasspath`로 **실제 해석된(resolved)** 버전을 직접 확인했고(직접 선언 버전이 아니라 Spring Boot BOM으로 관리되는 전이 의존성까지 포함), `WebSearch`로 각 핵심 라이브러리의 2026년 최신 CVE를 실시간 조회했다. 모든 항목은 조회 시점(2026-08-24) 기준이며, 그 이후 새로 공개되는 CVE는 재확인이 필요하다.

---

### [카테고리: 보안 / Medium] mariadb-java-client 3.5.1 — CVE-2026-55856 등 (조건부 악용 가능)

- **위치**: `build.gradle:29` (`implementation 'org.mariadb.jdbc:mariadb-java-client:3.5.1'`), Gradle 해석 결과에서도 오버라이드 없이 `3.5.1` 그대로 사용됨
- **문제**: `sslMode=verify-full` 또는 `verify-ca`로 접속하면서 서버 인증서를 pinning(`serverSslCert`/truststore)하지 않은 상태로 비밀번호 인증을 사용할 경우, 핸드셰이크 초기 단계에서 인증서 지문(fingerprint) 검증이 누락되어 중간자(MITM) 공격자가 자체 서명 인증서로 서버를 가장할 수 있다. 패치 버전은 3.5.9(2.x 라인은 2.7.14, 3.3.5, 3.4.3).
- **코드 근거**:
  ```gradle
  implementation 'org.mariadb.jdbc:mariadb-java-client:3.5.1'
  ```
- **판단 근거(왜 위험한지)**: HeroDevs 보안 공지("CVE-2026-55856 Through 55860 and CVE-2026-61700: MariaDB Connector CVEs in Spring Boot", 2026)에 따르면 이 취약점은 `sslMode=verify-full/verify-ca` + 비밀번호 인증 + 인증서 미고정 + 능동적 MITM 위치 확보라는 여러 조건이 동시에 충족되어야 악용 가능하다. 이 프로젝트는 설정 파일이 없어(`config-audit` Critical 항목 참고) 실제 `sslMode` 값을 확인할 수 없으므로, 조건 충족 여부는 "확인 불가"이며 라이브러리 자체는 알려진 취약 버전대에 있다는 사실만 확정적으로 기록한다. (실시간 조회 결과, 모델 학습 시점 이후의 정보이므로 재확인 권장)

### [카테고리: 보안 / Low] Spring Security 7.1.0 — CVE-2026-59270 (임베디드 LDAP 서버, 기능 미사용으로 위험도 낮음)

- **위치**: Gradle 해석 결과 `org.springframework.security:spring-security-core/config/web/crypto:7.1.0`
- **문제**: 2026-08-20(4일 전)에 공개된 CVE로, Spring Security에 내장된 UnboundID 테스트용 LDAP 서버 기능을 사용할 경우 잘 알려진 관리자 bind DN으로 인증이 가능해 인메모리 LDAP 디렉터리를 읽거나 변조할 수 있다. 패치 버전은 7.1.1(또는 7.0.7).
- **코드 근거**: `Grep` 결과 이 프로젝트 어디에도 LDAP 관련 설정/의존성(`spring-security-ldap`, `UnboundIdContainer` 등)이 없다.
- **판단 근거(왜 위험한지)**: 공식 Spring Security 보안 공지 기준 이 취약점은 임베디드 LDAP 서버 기능을 명시적으로 구성한 애플리케이션에만 해당된다. 이 프로젝트는 해당 기능을 사용하지 않으므로 현재는 실질적 위험이 낮지만, 공개된 지 얼마 되지 않은 CVE이고 같은 아티팩트(spring-security-core 등)의 마이너 버전 문제이므로 다음 정기 업데이트 시 7.1.1 이상으로 갱신하는 것을 권장 참고 사항으로 남긴다.

### [카테고리: 보안 / Info] 버전 확인됨, 알려진 취약점 없음 — Spring Boot / Spring Framework

- Spring Boot: **4.1.0** (2026-06-10 GA 확인됨). CVE-2026-40976(Actuator 인가 누락, CVSS 9.1)은 Spring Boot 4.0.0~4.0.5만 해당하고, 이 프로젝트는 (a) `spring-boot-actuator`를 의존하지 않고 (b) 커스텀 `SecurityConfig`가 이미 존재해 두 조건 모두 불충족 → 해당 없음.
- Spring Framework(spring-core/web/webmvc 등): **7.0.8** — 2026-03-19 공개된 CVE-2026-22732(HTTP 보안 헤더 누락, CVSS 9.1, 영향 범위 ~7.0.3)의 패치 버전(7.0.4+)보다 최신이라 해당 없음.

### [카테고리: 보안 / Info] 버전 확인됨, 알려진 취약점 없음 — Apache Tomcat (임베디드)

- **위치**: `tomcat-embed-core/el/websocket:11.0.22`
- CVE-2026-43513/43514/43515는 11.0.22에서 이미 수정됨. CVE-2026-50229(XSS)는 11.0.23에서 수정되는 항목이지만, 해당 취약점은 Tomcat 배포판에 포함된 "number guess" **예제 웹앱**에서만 발생하며 Spring Boot의 `spring-boot-starter-tomcat`은 예제 웹앱을 포함하지 않는 임베디드 코어만 사용하므로 이 프로젝트에는 해당하지 않는다.

### [카테고리: 보안 / Info] 버전 확인됨, 알려진 취약점 없음 — Jackson (com.fasterxml 2.x / tools.jackson 3.x)

- `jjwt-jackson`이 사용하는 `com.fasterxml.jackson.core:jackson-databind`는 `2.21.4`로 해석됨(레거시 취약 버전 2.12.7.1을 자동으로 상위 치환). Spring 자체 직렬화가 사용하는 `tools.jackson.core:jackson-databind`는 `3.1.4`로, Flyway 관련 CVE-2026-54512에서 요구하는 패치 버전(3.1.1 → 3.1.4)과 일치한다.

### [카테고리: 보안 / Info] 버전 확인됨, 알려진 취약점 없음 — Flyway

- `flyway-core`/`flyway-mysql`이 `12.4.0`으로 해석됨. 조회된 Flyway 관련 CVE들은 (a) Oracle JDK 자체 CVE, (b) Docker 이미지에 번들된 Couchbase/Netty 관련(이 프로젝트는 Couchbase 미사용), (c) Jackson 관련(위 항목에서 이미 패치 버전으로 확인)으로, 이 프로젝트의 사용 방식(MariaDB 마이그레이션, 라이브러리 형태 사용)에는 해당하지 않는다.

### [카테고리: 보안 / Low] jjwt 0.12.6 — 직접적 CVE는 없으나 legacy 버전으로 표시됨

- **위치**: `build.gradle:31-33`
- Snyk 등 주요 취약점 DB에서 `jjwt-root 0.12.6` 자체의 직접 CVE는 확인되지 않았다. 다만 Maven Central에서 0.12.6은 legacy로 표시되고 있고, 0.13.0이 최신 버전으로 안내되고 있다. 과거 0.11.2에서 발견된 CVE-2024-31033(0.12.6 이전 버전 대상)은 이미 해결된 상태다. 취약점이 아니라 "최신 버전 아님" 수준의 참고 사항이다.

### [카테고리: 보안 / Info] 문제 없음 — Lombok

- `1.18.46`으로 해석됨. 컴파일 타임에만 사용되는 도구로 런타임 CVE 이력이 없다.
