# secret-audit 결과

git 히스토리 점검: 가능 (`.git` 디렉터리 존재, `git log --all --full-history`로 전체 브랜치·전체 기간 확인). 결과: `application.yml`/`application.properties`/`application-*.yml`이 현재는 물론 과거 어떤 커밋에도 추적(tracked)된 적이 없다.

총평: 소스코드 내 하드코딩된 시크릿은 발견되지 않았다. 오히려 `.gitignore`에 `application.yml`이 명시되어 있어(26번째 줄), 개발자가 로컬에는 설정 파일을 두되 의도적으로 git 추적에서 제외한 것으로 보인다 — 이는 `config-audit`/`jwt-audit`에서 지적한 "설정 파일 부재"가 실수가 아니라 의도된 시크릿 관리 방식임을 시사하는 중요한 근거다. 다만 온보딩용 예시 설정 파일이 없다.

---

### [카테고리: 보안 / Info] 하드코딩된 민감정보 없음 — 소스코드 전체

- **조사 방법**: `password\s*=\s*"`, `secret\s*=\s*"`, `apiKey`, `access_key`, AWS 액세스 키 패턴(`AKIA...`), PEM 개인키 헤더, `System.out.println`, 민감정보를 포함할 수 있는 로그 호출 패턴을 대소문자 무관하게 전체 소스에서 검색했으나 매치되는 코드가 없었다.
- **판단 근거**: 발견된 것이 없으므로 심각도 판단 대상 자체가 없다.

### [카테고리: 보안 / Info] 중요 정황 — `.gitignore`에 `application.yml`이 명시되어 있음

- **위치**: `.gitignore:26`
- **문제(정황)**: `application.yml` 한 줄이 다른 IDE/빌드 관련 규칙들 사이에 단독으로 존재한다. 이는 로컬 환경에는 DB 접속정보·JWT 시크릿이 담긴 `application.yml`이 실제로 존재하며, 개발자가 이를 git에 커밋하지 않으려고 의도적으로 제외했을 가능성이 높다는 정황 증거다.
- **코드 근거**:
  ```
  ### IntelliJ IDEA ###
  .idea
  ...
  application.yml
  ```
- **판단 근거(왜 기록하는지)**: 시크릿을 git에 커밋하지 않는 것 자체는 OWASP Secrets Management Cheat Sheet가 권장하는 올바른 방향이다. 다만 `git log --all --full-history -- "**/application.yml"`로 전체 히스토리를 확인한 결과 이 파일이 과거에도 추적된 적이 없어(한 번도 커밋되지 않음) 히스토리상의 유출 흔적은 없다. 이 사실은 `config-audit`의 "설정 파일 완전 부재" Critical 항목과 `jwt-audit`의 "서명 키 값 확인 불가" 항목의 성격을 바꾼다 — "빠뜨림"이 아니라 "의도적 제외"일 가능성이 높으므로, 4~5단계 대화에서 사용자에게 이 부분이 의도된 것인지 반드시 확인이 필요하다.

### [카테고리: 보안 / Low] 온보딩/배포용 예시 설정 파일(`application.yml.example` 등) 부재

- **위치**: 프로젝트 전체
- **문제**: 설정 파일을 git에서 제외하는 것은 올바르지만, 어떤 키가 필요한지 보여주는 예시 파일(`application-example.yml`, `application.yml.sample` 등)이나 `README`의 설정 안내가 없다. 새 개발자나 배포 파이프라인이 무엇을 채워야 하는지 알 방법이 코드(`JwtProperties`, `spring.datasource.*` 요구사항)를 직접 읽는 것 말고는 없다.
- **판단 근거**: 일반적인 시크릿 관리 원칙상(OWASP Secrets Management Cheat Sheet), 실제 값은 커밋하지 않되 "어떤 키가 필요한지"에 대한 템플릿은 별도로 관리하는 것이 팀 협업과 배포 안정성 측면에서 권장된다. 보안 취약점이라기보다 운영/협업 편의성 항목이다.

### [카테고리: 보안 / Info] 문제 없음 — 런타임 로그를 통한 민감정보 노출

- **위치**: `GlobalExceptionHandler.java:32-34, 43, 54, 63`
- `log.warn`/`log.error`가 예외 메시지를 기록하지만, 이 메시지들은 `ErrorCode` enum에 정의된 고정 문구(예: "잘못된 입력값입니다")이거나 Bean Validation의 필드 오류 메시지이며, 비밀번호·토큰 원문이 로그에 찍히는 경로는 없다.

### [카테고리: 보안 / Info] 문제 없음 (원칙적으로 안전한 구조) — 키 관리 방식

- **위치**: `JwtProperties.java`
- JWT 서명 키가 `@ConfigurationProperties`를 통해 외부 설정에서 주입되도록 구조화되어 있어, 코드 변경 없이 키 교체가 가능하고 소스코드 저장소에 키가 남지 않는다. 실제 운영 환경에서 이 값을 Vault/AWS Secrets Manager 등으로 관리하는지, 아니면 서버의 로컬 `application.yml`로만 관리하는지는 저장소 범위 밖이라 확인할 수 없다.
