# input-validation-audit 결과

총평: DB 접근이 전부 Spring Data JPA의 파생 쿼리 메서드로만 이루어져 SQL Injection 벡터는 발견되지 않았고, 파일 업로드·명령 실행·XML 파싱 코드 자체가 없어 해당 카테고리는 점검 대상이 없다. 다만 Controller가 아직 없어 `@Valid` 실제 적용 여부는 확인 불가하며, DTO의 길이 제약이 Entity의 DB 컬럼 제약과 어긋나는 부분이 있다.

---

### [카테고리: 보안 / Info] 문제 없음 — SQL Injection

- **위치**: `src/main/java/com/example/quick_gg/repository/StudentRepository.java`, `RefreshTokenRepository.java`
- 모든 쿼리가 Spring Data JPA의 메서드 이름 기반 파생 쿼리(`findByStudentNumber`, `existsBySummonerNameAndTag` 등)이며, `@Query(nativeQuery = true)`나 문자열 연결로 쿼리를 조립하는 코드가 없다.
- **판단 근거**: Spring Data JPA 공식 문서상 파생 쿼리는 내부적으로 파라미터 바인딩(`PreparedStatement`)을 사용하므로 CWE-89(SQL Injection) 벡터가 없다.

### [카테고리: 보안 / Info] 해당 없음 — 파일 업로드 / Command Injection / XXE·역직렬화

- `MultipartFile`, `Runtime.exec`, `ProcessBuilder`, `DocumentBuilderFactory`, `SAXParserFactory`, Jackson 다형적 역직렬화(`@JsonTypeInfo`, `enableDefaultTyping`) 관련 코드가 프로젝트 전체에 존재하지 않는다 (`Grep` 전체 무매칭). 현재 코드 범위에서는 점검 대상이 없다.

### [카테고리: 보안 / Info] 평가 보류 — `@Valid` 적용 여부

- Controller 자체가 아직 구현되어 있지 않아(`project-analysis.md` 참고) `@RequestBody`에 `@Valid`를 붙이는지 확인할 수 없다. `LoginRequest`/`SignupRequest`에 Bean Validation 어노테이션은 선언되어 있으나, 이를 실제로 트리거하는 Controller 계층이 생기는 시점에 재점검이 필요하다.

### [카테고리: 보안 / Medium] DTO 필드 길이 제약이 Entity의 DB 컬럼 길이와 불일치

- **위치**: `src/main/java/com/example/quick_gg/dto/request/SignupRequest.java:13-40`, `src/main/java/com/example/quick_gg/entity/StudentEntity.java:30-46`
- **문제**: `SignupRequest`의 `studentID`, `name`, `summonerName`, `tag` 필드는 `@NotBlank`만 있고 `@Size` 상한이 없다. 반면 `StudentEntity`는 각각 `length = 10`, `20`, `50`, `10`으로 DB 컬럼 길이가 제한되어 있다. 상한을 넘는 값이 들어오면 Bean Validation 단계를 통과해 서비스 로직까지 진행된 뒤 JPA/DB 레벨에서야 예외(`DataIntegrityViolationException` 등)가 발생한다.
- **코드 근거**:
  ```java
  // SignupRequest.java
  @NotBlank(message = "학번을 입력해주세요")
  private String studentID; // 길이 상한 없음

  // StudentEntity.java
  @Column(name = "student_number", nullable = false, unique = true, length = 10)
  private String studentID; // DB 컬럼은 10자로 제한
  ```
- **판단 근거(왜 위험한지)**: CWE-20(Improper Input Validation) 관점에서, 입력 검증은 실제 사용 지점(DB 스키마)의 제약과 일치해야 사용자에게 명확한 400 에러를 줄 수 있고 예기치 않은 DB 예외로 인한 500 처리를 피할 수 있다. 현재는 `GlobalExceptionHandler`의 `Exception.class` 캐치올이 있어 애플리케이션이 죽지는 않지만(→ `exception-audit`에서 별도 확인), 사용자는 "학번을 입력해주세요"가 아닌 알 수 없는 서버 오류 메시지를 받게 된다.

### [카테고리: 보안 / Low] 비밀번호 길이 제약이 DTO 선언과 서비스 로직에서 서로 다름

- **위치**: `SignupRequest.java:22-23` (`@Size(min = 2, max = 20)`) vs `SignupService.java:34-38` (`len < 6 || len > 20`이면 예외)
- **문제**: Bean Validation은 2자 이상이면 통과시키지만, 서비스 로직은 6자 미만이면 다시 거부한다. 두 검증 계층의 기준이 달라 실제 정책(최소 6자)이 DTO 어노테이션만 봐서는 드러나지 않는다.
- **판단 근거**: 일반적인 입력값 검증 원칙상, 동일한 값에 대한 제약은 한 곳에서 일관되게 선언되는 것이 유지보수성과 예측 가능성 측면에서 안전하다. 현재는 보안 취약점으로 이어지지는 않지만(오히려 서비스 쪽이 더 엄격), 두 계층의 기준 불일치는 향후 한쪽만 수정했을 때 검증 우회로 이어질 수 있는 잠재 요인이다.
